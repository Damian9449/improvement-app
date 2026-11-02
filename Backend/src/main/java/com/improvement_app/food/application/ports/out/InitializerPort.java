package com.improvement_app.food.application.ports.out;

import java.io.IOException;

public interface InitializerPort {

    void initializeData(String username) throws IOException;

}
