package com.chaekchaek.member.service;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AnonymousHandleGenerator {

    public String generate() {
        return "참새-" + UUID.randomUUID()
                .toString()
                .substring(0, 8);
    }
}
