package com.humolix.volur.framework.id.jpa;

import com.humolix.volur.framework.common.util.SpringContextHolder;
import com.humolix.volur.framework.id.annotation.SnowflakeGenerated;
import com.humolix.volur.framework.id.generator.SnowflakeIdGenerator;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.id.IdentifierGenerator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.lang.reflect.Member;
import java.util.EnumSet;

@Component
public class SnowflakeIdentifierGenerator implements BeforeExecutionGenerator, IdentifierGenerator, ApplicationContextAware {

    private static volatile SnowflakeIdGenerator snowflakeIdGenerator;

    public SnowflakeIdentifierGenerator() {
    }

    public SnowflakeIdentifierGenerator(SnowflakeGenerated config, Member member, GeneratorCreationContext context) {
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) {
        snowflakeIdGenerator = applicationContext.getBean(SnowflakeIdGenerator.class);
    }

    @Override
    public Object generate(SharedSessionContractImplementor session, Object owner, Object currentValue, EventType eventType) {
        if (currentValue != null) {
            return currentValue;
        }
        return generateNextId();
    }

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object object) {
        return generateNextId();
    }

    private Serializable generateNextId() {
        if (snowflakeIdGenerator == null) {
            synchronized (SnowflakeIdentifierGenerator.class) {
                if (snowflakeIdGenerator == null) {
                    snowflakeIdGenerator = SpringContextHolder.getBean(SnowflakeIdGenerator.class);
                }
            }
        }
        if (snowflakeIdGenerator == null) {
            throw new IllegalStateException("SnowflakeIdGenerator is not initialized in Spring Context");
        }
        return snowflakeIdGenerator.nextId();
    }

    @Override
    public EnumSet<EventType> getEventTypes() {
        return EventTypeSets.INSERT_ONLY;
    }

}
