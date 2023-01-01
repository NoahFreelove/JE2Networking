package org.networking;

public record Command (Role role, String command, String args, CommandExec exec) {
}
