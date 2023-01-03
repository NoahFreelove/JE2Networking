package JE.networking.Commands;

public record Command (Role role, String command, String args, CommandExec exec) {
}
