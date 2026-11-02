package com.improvement_app.food.application.ports.in;

import java.io.IOException;

public interface InitializationUseCase {
    void initializeData(String username) throws IOException;
}
