package com.airplane.controller;

import com.airplane.dto.FeedbackRequest;
import com.airplane.feedback.FeedbackService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping("/feedback")
    public ResponseEntity<Void> feedback(@Valid @RequestBody FeedbackRequest req) {
        feedbackService.recordFeedback(req.rating(), req.question(), req.orgCode(), req.mode(), req.sourceIds());
        return ResponseEntity.noContent().build();
    }

    /** 운영 지표. 로컬 실행 전용이므로 루프백(같은 PC) 요청만 허용한다. */
    @GetMapping("/insights")
    public ResponseEntity<FeedbackService.Insights> insights(@RequestParam(defaultValue = "10") int limit,
                                                             HttpServletRequest request) {
        if (!isLoopback(request.getRemoteAddr())) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(feedbackService.insights(Math.max(1, Math.min(limit, 50))));
    }

    private static boolean isLoopback(String addr) {
        try {
            return InetAddress.getByName(addr).isLoopbackAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
