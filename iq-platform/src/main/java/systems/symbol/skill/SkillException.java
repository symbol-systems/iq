package systems.symbol.skill;

import jakarta.ws.rs.core.Response;

public class SkillException extends Throwable {
    int status;
    public SkillException(String s, Response.Status status) {
        super(s);
        this.status = status.getStatusCode();
    }

    public SkillException(String s, int status) {
        super(s);
        this.status = status;
    }

    public int getStatus() {
        return this.status;
    }
}
