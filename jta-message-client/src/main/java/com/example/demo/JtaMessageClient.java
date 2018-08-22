package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

@SpringBootApplication
public class JtaMessageClient {

	public static void main(String[] args) {
		SpringApplication.run(JtaMessageClient.class, args);
	}


    @Service
    public static class JmsListenerInstance {

        @JmsListener(destination = "message")
        public void onNewMessage(String message) {
            System.out.println("messageId: " + message);
        }

    }

}
