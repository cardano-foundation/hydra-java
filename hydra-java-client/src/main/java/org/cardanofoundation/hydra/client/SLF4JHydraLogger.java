package org.cardanofoundation.hydra.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import org.cardanofoundation.hydra.core.model.HydraState;
import org.cardanofoundation.hydra.core.model.query.response.Response;
import org.slf4j.Logger;

@Builder(builderMethodName = "hiddenBuilder")
@AllArgsConstructor
public class SLF4JHydraLogger extends HydraQueryEventListener.Stub implements HydraStateEventListener {

    private final Logger logger;
    private final String actor;
    private final boolean warnOnFailure = true;

    public static SLF4JHydraLoggerBuilder builder(Logger logger, String actor) {
        return hiddenBuilder().logger(logger).actor(actor);
    }

    public static SLF4JHydraLogger of(Logger logger, String actor) {
        return SLF4JHydraLogger.builder(logger, actor).build();
    }

    @Override
    public void onSuccess(Response response) {
        logger.info(msg(response));
    }

    @Override
    public void onFailure(Response response) {
        if (warnOnFailure) {
            logger.warn(msg(response));
        } else {
            logger.error(msg(response));
        }
    }

    private String msg(Response response) {
        return String.format("[%s]:%s", actor, response.toString());
    }

    @Override
    public void onStateChanged(HydraState prevState, HydraState newState) {
        logger.info("actor:{}, prev:{}, now:{}", actor, prevState, newState);
    }

}
