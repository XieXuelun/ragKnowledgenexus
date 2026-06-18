package com.xxr.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xxr.kb.pojo.KbFavorite;
import com.xxr.mapper.KbFavoriteMapper;
import com.xxr.service.KbFavoriteService;
import org.springframework.stereotype.Service;

@Service
public class kbFavoriteServiceImpl extends ServiceImpl<KbFavoriteMapper, KbFavorite> implements KbFavoriteService {
}
