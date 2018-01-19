package org.apereo.cas.memcached;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.DefaultHashAlgorithm;
import net.spy.memcached.FailureMode;
import net.spy.memcached.MemcachedClientIF;
import net.spy.memcached.spring.MemcachedClientFactoryBean;
import net.spy.memcached.transcoders.Transcoder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apereo.cas.configuration.model.support.memcached.BaseMemcachedProperties;

/**
 * This is {@link MemcachedPooledClientConnectionFactory}.
 *
 * @author Misagh Moayyed
 * @since 5.2.0
 */
@Slf4j
@AllArgsConstructor
public class MemcachedPooledClientConnectionFactory extends BasePooledObjectFactory<MemcachedClientIF> {

    private final BaseMemcachedProperties memcachedProperties;
    private final Transcoder transcoder;

    @Override
    @SneakyThrows
    public MemcachedClientIF create() {
        final MemcachedClientFactoryBean factoryBean = new MemcachedClientFactoryBean();
        factoryBean.setServers(memcachedProperties.getServers());
        factoryBean.setTranscoder(this.transcoder);

        if (StringUtils.isNotBlank(memcachedProperties.getLocatorType())) {
            factoryBean.setLocatorType(ConnectionFactoryBuilder.Locator.valueOf(memcachedProperties.getLocatorType()));
        }
        if (StringUtils.isNotBlank(memcachedProperties.getFailureMode())) {
            factoryBean.setFailureMode(FailureMode.valueOf(memcachedProperties.getFailureMode()));
        }
        if (StringUtils.isNotBlank(memcachedProperties.getHashAlgorithm())) {
            factoryBean.setHashAlg(DefaultHashAlgorithm.valueOf(memcachedProperties.getHashAlgorithm()));
        }

        factoryBean.setDaemon(memcachedProperties.isDaemon());
        factoryBean.setShouldOptimize(memcachedProperties.isShouldOptimize());
        factoryBean.setUseNagleAlgorithm(memcachedProperties.isUseNagleAlgorithm());

        if (memcachedProperties.getOpTimeout() > 0) {
            factoryBean.setOpTimeout(memcachedProperties.getOpTimeout());
        }
        if (memcachedProperties.getMaxReconnectDelay() > 0) {
            factoryBean.setMaxReconnectDelay(memcachedProperties.getMaxReconnectDelay());
        }
        if (memcachedProperties.getShutdownTimeoutSeconds() > 0) {
            factoryBean.setShutdownTimeoutSeconds(memcachedProperties.getShutdownTimeoutSeconds());
        }
        if (memcachedProperties.getTimeoutExceptionThreshold() > 0) {
            factoryBean.setTimeoutExceptionThreshold(memcachedProperties.getTimeoutExceptionThreshold());
        }
        factoryBean.afterPropertiesSet();
        return (MemcachedClientIF) factoryBean.getObject();
    }

    @Override
    public PooledObject<MemcachedClientIF> wrap(final MemcachedClientIF memcachedClientIF) {
        return new DefaultPooledObject<>(memcachedClientIF);
    }

    @Override
    public void destroyObject(final PooledObject<MemcachedClientIF> p) {
        try {
            p.getObject().shutdown();
            p.invalidate();
        } catch (final Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }


    /**
     * Gets object pool.
     *
     * @return the object pool
     */
    public ObjectPool<MemcachedClientIF> getObjectPool() {
        final GenericObjectPool<MemcachedClientIF> pool = new GenericObjectPool<>(this);
        pool.setMaxIdle(memcachedProperties.getMaxIdle());
        pool.setMinIdle(memcachedProperties.getMinIdle());
        pool.setMaxTotal(memcachedProperties.getMaxTotal());
        return pool;
    }
}
