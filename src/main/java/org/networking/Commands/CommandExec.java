package org.networking.Commands;

import org.networking.Client;

public interface CommandExec {
    void execute(String[] args, Client initiatedBy);

}
