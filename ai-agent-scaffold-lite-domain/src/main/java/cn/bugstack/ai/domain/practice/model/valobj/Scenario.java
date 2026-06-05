package cn.bugstack.ai.domain.practice.model.valobj;

/**
 * 口语练习场景枚举
 */
public enum Scenario {

    INTERVIEW("interview", "Senior Tech Interview", "You are a senior tech interviewer. Ask questions, correct grammar, respond naturally."),
    RESTAURANT("restaurant", "Restaurant Ordering", "You are a waiter. Greet customer, take order, correct grammar."),
    MEETING("meeting", "Business Meeting", "You are a colleague in a business meeting. Lead discussion, correct grammar.");

    private final String code;
    private final String name;
    private final String systemPrompt;

    Scenario(String code, String name, String systemPrompt) {
        this.code = code;
        this.name = name;
        this.systemPrompt = systemPrompt;
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public String getSystemPrompt() { return systemPrompt; }

    public static Scenario fromCode(String code) {
        for (Scenario s : values()) {
            if (s.code.equalsIgnoreCase(code)) return s;
        }
        return INTERVIEW;
    }
}
