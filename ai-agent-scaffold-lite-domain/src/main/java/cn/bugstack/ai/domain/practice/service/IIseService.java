package cn.bugstack.ai.domain.practice.service;

import cn.bugstack.ai.domain.practice.model.valobj.IseResult;

/**
 * ISE 发音评测服务接口
 */
public interface IIseService {
    IseResult evaluate(byte[] audioData, String referenceText);
}
