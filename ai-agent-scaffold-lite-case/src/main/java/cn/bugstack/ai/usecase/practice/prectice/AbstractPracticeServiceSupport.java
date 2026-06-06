package cn.bugstack.ai.usecase.practice.prectice;

import cn.bugstack.ai.domain.practice.model.valobj.HandlePracticeMessageCommandEntity;
import cn.bugstack.ai.domain.practice.model.valobj.PracticeResult;
import cn.bugstack.ai.usecase.practice.prectice.factory.DefaultPracticeFactory;
import cn.bugstack.wrench.design.framework.tree.AbstractMultiThreadStrategyRouter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public abstract class AbstractPracticeServiceSupport extends AbstractMultiThreadStrategyRouter<HandlePracticeMessageCommandEntity,DefaultPracticeFactory.DynamicContext , PracticeResult>{

    @Override
    protected void multiThread(HandlePracticeMessageCommandEntity requestParameter, DefaultPracticeFactory.DynamicContext dynamicContext)  throws ExecutionException, InterruptedException, TimeoutException {
    }
}
