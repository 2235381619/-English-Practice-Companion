package cn.bugstack.ai.api.practice.dto;

import lombok.Data;

import java.util.List;

@Data
public class SubmitAudioResponse {
    private String asrText;
    private String correctedText;
    private List<String> grammarIssues;
    private List<String> suggestions;
    private int score;
    private String aiReply;
}
