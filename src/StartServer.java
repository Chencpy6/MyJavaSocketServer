

public class StartServer {
	private static SocketServer socketServer;
	public static void main(String[] args) {
		socketServer = new SocketServer();
		socketServer.start();
	}

}
