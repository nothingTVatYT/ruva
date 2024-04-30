package net.nothingtv.ruva.client.ui;

public class TestConsoleListener implements ConsoleListener {
    @Override
    public void handle(String msg) {
        PlayerConsole.println(msg);
    }
}
