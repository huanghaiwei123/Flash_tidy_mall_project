package com.gdou.captcha.pojo;

public class CaptchaVO {
    private String answer;
    private String image;
    private String expression;

    public CaptchaVO() {}

    public CaptchaVO(String answer, String image, String expression) {
        this.answer = answer;
        this.image = image;
        this.expression = expression;
    }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public String getExpression() { return expression; }
    public void setExpression(String expression) { this.expression = expression; }
}
