package com.improvement_app.food.application;

import com.improvement_app.food.application.ports.in.InitializationUseCase;
import com.improvement_app.food.application.ports.out.InitializerPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class InitializationDataService implements InitializationUseCase {

    private final InitializerPort initializerPort;

    public void initializeData(String username) throws IOException {
        initializerPort.initializeData(username);
    }

}
