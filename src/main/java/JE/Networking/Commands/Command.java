package JE.Networking.Commands;

public record Command (Role role, String command, String args, CommandExec exec) {
}
