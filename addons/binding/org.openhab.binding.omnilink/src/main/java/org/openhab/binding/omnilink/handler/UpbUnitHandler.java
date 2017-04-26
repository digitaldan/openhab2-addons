package org.openhab.binding.omnilink.handler;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.smarthome.core.library.types.IncreaseDecreaseType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.UID;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.Type;
import org.openhab.binding.omnilink.OmnilinkBindingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.digitaldan.jomnilinkII.MessageTypes.statuses.UnitStatus;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

public class UpbUnitHandler extends AbstractOmnilinkHandler implements UnitHandler {

    public static final int UNIT_OFF = 0;
    public static final int UNIT_ON = 1;
    public static final int UNIT_SCENE_A = 2;
    public static final int UNIT_SCENE_L = 13;
    public static final int UNIT_DIM_1 = 17;
    public static final int UNIT_DIM_9 = 25;
    public static final int UNIT_BRIGHTEN_1 = 33;
    public static final int UNIT_BRIGHTEN_9 = 41;
    public static final int UNIT_LEVEL_0 = 100;
    public static final int UNIT_LEVEL_100 = 200;

    private static Map<Type, OmniLinkCmd> sCommandMappingMap = new HashMap<Type, OmniLinkCmd>();

    static {
        sCommandMappingMap.put(IncreaseDecreaseType.INCREASE, OmniLinkCmd.CMD_UNIT_UPB_BRIGHTEN_STEP_1);
        sCommandMappingMap.put(IncreaseDecreaseType.DECREASE, OmniLinkCmd.CMD_UNIT_UPB_DIM_STEP_1);
        sCommandMappingMap.put(OnOffType.ON, OmniLinkCmd.CMD_UNIT_ON);
        sCommandMappingMap.put(OnOffType.OFF, OmniLinkCmd.CMD_UNIT_OFF);
    }

    public UpbUnitHandler(Thing thing) {
        super(thing);
    }

    private Logger logger = LoggerFactory.getLogger(UpbUnitHandler.class);

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {

        String[] channelParts = channelUID.getAsString().split(UID.SEPARATOR);
        logger.debug("handleCommand called");
        OmniLinkCmd omniCmd;
        if (command instanceof PercentType) {
            omniCmd = OmniLinkCmd.CMD_UNIT_PERCENT;
            getOmnilinkBridgeHander().sendOmnilinkCommand(omniCmd.getNumber(), ((PercentType) command).intValue(),
                    Integer.parseInt(channelParts[2]));
        } else if (command instanceof RefreshType) {
            logger.debug("Unit '{}' got REFRESH command", thing.getLabel());
            Futures.addCallback(getOmnilinkBridgeHander().getUnitStatus(Integer.parseInt(channelParts[2])),
                    new FutureCallback<UnitStatus>() {
                        @Override
                        public void onFailure(Throwable arg0) {
                            logger.error("Error refreshing unit status", arg0);
                        }

                        @Override
                        public void onSuccess(UnitStatus status) {
                            handleUnitStatus(status);
                        }
                    });
        } else {
            omniCmd = sCommandMappingMap.get(command);
            getOmnilinkBridgeHander().sendOmnilinkCommand(omniCmd.getNumber(), 0, Integer.parseInt(channelParts[2]));
        }

    }

    @Override
    public void handleUnitStatus(UnitStatus unitStatus) {

        // TODO is dimmer, or just simple switch
        // assuming dimmer right now.
        logger.debug("need to handle status update{}", unitStatus);
        int status = unitStatus.getStatus();
        int level = 0;
        if (status == UNIT_ON) {
            level = 100;
        } else if ((status >= UNIT_SCENE_A) && (status <= UNIT_SCENE_L)) {
            level = 100;
        } else if ((status >= UNIT_LEVEL_0) && (status <= UNIT_LEVEL_100)) {
            level = status - UNIT_LEVEL_0;
        }

        State newState = PercentType.valueOf(Integer.toString(level));

        logger.debug("handle Zone Status Change to: " + newState);
        updateState(OmnilinkBindingConstants.CHANNEL_LIGHTLEVEL, newState);

    }
}
