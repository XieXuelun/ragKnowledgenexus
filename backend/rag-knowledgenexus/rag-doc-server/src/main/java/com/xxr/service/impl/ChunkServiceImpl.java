package com.xxr.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xxr.document.pojo.DocChunk;
import com.xxr.mapper.ChunkMapper;
import com.xxr.service.ChunkService;
import org.springframework.stereotype.Service;

@Service
public class ChunkServiceImpl extends ServiceImpl<ChunkMapper, DocChunk> implements ChunkService {
}
