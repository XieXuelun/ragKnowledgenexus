package com.xxr.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xxr.document.pojo.DocDocument;
import com.xxr.mapper.*;
import com.xxr.qa.pojo.QaConversation;
import com.xxr.qa.pojo.QaMessage;
import com.xxr.stat.pojo.StatDaily;
import com.xxr.stat.pojo.StatHotQuestion;
import com.xxr.ticket.pojo.TicketOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatScheduledTask {

    private final QaMessageMapper qaMessageMapper;
    private final QaConversationMapper qaConversationMapper;
    private final TicketOrderMapper ticketOrderMapper;
    private final DocumentMapper documentMapper;
    private final StatDailyMapper statDailyMapper;
    private final StatHotQuestionMapper statHotQuestionMapper;

    private static final int MAX_HOT_QUESTIONS = 50;

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void computeDailyStats() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("computeDailyStats start for date={}", yesterday);
        try {
            StatDaily daily = computeStatDaily(yesterday);
            upsertStatDaily(daily);
            List<StatHotQuestion> hotQuestions = computeHotQuestions(yesterday);
            upsertHotQuestions(hotQuestions);
            log.info("computeDailyStats done: qa={} users={} rate={}% tickets={} resp={}ms docs={} hotQ={}",
                    daily.getTotalQa(), daily.getTotalUsers(),
                    daily.getPositiveRate(), daily.getTransferCount(),
                    daily.getAvgResponseMs(), daily.getNewDocs(), hotQuestions.size());
        } catch (Exception e) {
            log.error("computeDailyStats failed for date={}", yesterday, e);
            throw e;
        }
    }

    private StatDaily computeStatDaily(LocalDate date) {
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.atTime(LocalTime.MAX);

        LambdaQueryWrapper<QaMessage> qaWrapper = new LambdaQueryWrapper<>();
        qaWrapper.ge(QaMessage::getCreateTime, dayStart);
        qaWrapper.le(QaMessage::getCreateTime, dayEnd);
        List<QaMessage> qaMessages = qaMessageMapper.selectList(qaWrapper);

        StatDaily daily = new StatDaily();
        daily.setStatDate(date);
        daily.setTotalQa(qaMessages.size());
        daily.setTotalUsers((int) qaMessages.stream().map(QaMessage::getUserId).filter(Objects::nonNull).distinct().count());

        long scored = qaMessages.stream().filter(m -> m.getQualityScore() != null).count();
        long positive = qaMessages.stream().filter(m -> m.getQualityScore() != null && m.getQualityScore() > 0).count();
        daily.setPositiveRate(scored > 0
                ? BigDecimal.valueOf(positive).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(scored), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        LambdaQueryWrapper<TicketOrder> tw = new LambdaQueryWrapper<>();
        tw.ge(TicketOrder::getCreateTime, dayStart).le(TicketOrder::getCreateTime, dayEnd);
        daily.setTransferCount(ticketOrderMapper.selectCount(tw).intValue());

        List<QaMessage> withMs = qaMessages.stream().filter(m -> m.getResponseMs() != null).collect(Collectors.toList());
        daily.setAvgResponseMs(withMs.isEmpty() ? 0 : (int) Math.round(withMs.stream().mapToLong(QaMessage::getResponseMs).average().orElse(0)));

        LambdaQueryWrapper<DocDocument> dw = new LambdaQueryWrapper<>();
        dw.ge(DocDocument::getCreateTime, dayStart).le(DocDocument::getCreateTime, dayEnd);
        daily.setNewDocs(documentMapper.selectCount(dw).intValue());

        return daily;
    }

    private void upsertStatDaily(StatDaily daily) {
        LambdaQueryWrapper<StatDaily> w = new LambdaQueryWrapper<>();
        w.eq(StatDaily::getStatDate, daily.getStatDate());
        StatDaily existing = statDailyMapper.selectOne(w);
        if (existing != null) {
            daily.setId(existing.getId());
            daily.setCreateTime(existing.getCreateTime());
            daily.setUpdateTime(LocalDateTime.now());
            statDailyMapper.updateById(daily);
        } else {
            statDailyMapper.insert(daily);
        }
    }

    private List<StatHotQuestion> computeHotQuestions(LocalDate date) {
        LocalDateTime ds = date.atStartOfDay();
        LocalDateTime de = date.atTime(LocalTime.MAX);

        LambdaQueryWrapper<QaMessage> qw = new LambdaQueryWrapper<>();
        qw.ge(QaMessage::getCreateTime, ds).le(QaMessage::getCreateTime, de);
        qw.isNotNull(QaMessage::getQuestion);
        List<QaMessage> msgs = qaMessageMapper.selectList(qw);
        if (msgs.isEmpty()) return Collections.emptyList();

        Set<Long> cids = msgs.stream().map(QaMessage::getConversationId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Long> kbMap = new HashMap<>();
        if (!cids.isEmpty()) {
            LambdaQueryWrapper<QaConversation> cw = new LambdaQueryWrapper<>();
            cw.in(QaConversation::getId, cids);
            qaConversationMapper.selectList(cw).forEach(c -> kbMap.put(c.getId(), c.getKbId()));
        }

        MessageDigest md = getSha256();
        List<StatHotQuestion> result = new ArrayList<>();
        msgs.stream().filter(m -> m.getQuestion() != null && !m.getQuestion().isBlank())
                .collect(Collectors.groupingBy(m -> normalizeQuestion(m.getQuestion())))
                .values().stream().filter(g -> !g.isEmpty()).forEach(g -> {
            QaMessage f = g.get(0);
            StatHotQuestion hq = new StatHotQuestion();
            hq.setQuestionHash(bytesToHex(md.digest(normalizeQuestion(f.getQuestion()).getBytes())));
            hq.setQuestion(f.getQuestion());
            hq.setKbId(kbMap.get(f.getConversationId()));
            hq.setAskCount(g.size());
            g.stream().map(QaMessage::getCreateTime).filter(Objects::nonNull).max(LocalDateTime::compareTo).ifPresent(hq::setLastAskTime);
            double avg = g.stream().filter(m -> m.getQualityScore() != null).mapToInt(QaMessage::getQualityScore).average().orElse(0);
            hq.setAvgQuality(BigDecimal.valueOf((avg + 1) / 2).setScale(2, RoundingMode.HALF_UP));
            hq.setUpdateTime(LocalDateTime.now());
            result.add(hq);
        });

        result.sort((a, b) -> b.getAskCount().compareTo(a.getAskCount()));
        return result.size() > MAX_HOT_QUESTIONS ? result.subList(0, MAX_HOT_QUESTIONS) : result;
    }

    private void upsertHotQuestions(List<StatHotQuestion> list) {
        for (StatHotQuestion hq : list) {
            LambdaQueryWrapper<StatHotQuestion> w = new LambdaQueryWrapper<>();
            w.eq(StatHotQuestion::getQuestionHash, hq.getQuestionHash());
            if (hq.getKbId() != null) w.eq(StatHotQuestion::getKbId, hq.getKbId());
            else w.isNull(StatHotQuestion::getKbId);
            StatHotQuestion existing = statHotQuestionMapper.selectOne(w);
            if (existing != null) {
                existing.setAskCount(existing.getAskCount() + hq.getAskCount());
                if (hq.getLastAskTime() != null && (existing.getLastAskTime() == null || hq.getLastAskTime().isAfter(existing.getLastAskTime())))
                    existing.setLastAskTime(hq.getLastAskTime());
                statHotQuestionMapper.updateById(existing);
            } else {
                statHotQuestionMapper.insert(hq);
            }
        }
    }

    private static String normalizeQuestion(String q) { return q.trim().replaceAll("\\s+", " ").toLowerCase(); }

    private static MessageDigest getSha256() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException e) { throw new RuntimeException("SHA-256 not available", e); }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
