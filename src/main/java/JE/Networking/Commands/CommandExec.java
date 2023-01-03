package JE.Networking.Commands;

import JE.Networking.Client;

public interface CommandExec {
    void execute(String[] args, Client initiatedBy);

}
