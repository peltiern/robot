package fr.roboteek.robot.systemenerveux.event.persistance;

import com.google.common.eventbus.Subscribe;
import fr.roboteek.robot.spring.server.ContextProvider;
import fr.roboteek.robot.spring.server.model.RobotEventModel;
import fr.roboteek.robot.spring.server.repository.RobotEventRepository;
import fr.roboteek.robot.systemenerveux.event.*;
import org.springframework.beans.BeanUtils;

public class RobotEventPersistance {

    private final RobotEventRepository robotEventRepository;

    public RobotEventPersistance() {
        robotEventRepository = ContextProvider.getBean(RobotEventRepository.class);
    }

    @Subscribe
    public void handleRobotEvent(RobotEvent robotEvent) {
        if (robotEvent instanceof ConversationEvent || robotEvent instanceof ParoleEvent || robotEvent instanceof MouvementCouEvent) {
            robotEventRepository.save(robotEvent);
        }
    }
}
