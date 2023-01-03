package org.networking.Commands;

public record Command (Role role, String command, String args, CommandExec exec) {
}
