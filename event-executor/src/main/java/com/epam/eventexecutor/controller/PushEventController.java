package com.epam.eventexecutor.controller;

import com.epam.eventexecutor.model.ParticipantPushDetails;
import com.epam.eventexecutor.service.EventExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PushEventController {

    private final EventExecutionService eventService;

    @Autowired
    public PushEventController(EventExecutionService eventService) {
        this.eventService = eventService;
    }

    /**
     * Getting called when a user register for the event
     */
    @PostMapping(path = "participant/registration/push")
    public void participantRegistrationPushEvent(@RequestBody ParticipantPushDetails participantPushDetails){
        eventService.submitParticipantRegistrationEvent(participantPushDetails);
    }

    /**
     * Getting called when there is a push event for a user
     */
    @PostMapping(path = "participant/push")
    public void participantPushEvent(@RequestBody ParticipantPushDetails participantPushDetails){
        eventService.submitParticipantPushEvent(participantPushDetails);
    }
}
