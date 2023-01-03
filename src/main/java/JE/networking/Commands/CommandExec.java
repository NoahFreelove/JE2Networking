package JE.networking.Commands;

import JE.networking.Client;

public interface CommandExec {
    void execute(String[] args, Client initiatedBy);

}
