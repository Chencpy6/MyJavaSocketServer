import java.io.File;
import java.io.FileInputStream;

public class SendTool {
	public static void LoginReturn(User user,String succeedOrNot,String error) {
		if (error==null)error="";
		System.out.println("登录: "+succeedOrNot+" error: "+error);
		user.SendMessage(succeedOrNot, error, 1);
	}
	public static void RegisterReturn(User user,String succeedOrNot,String error) {
		if (error==null)error="";
		System.out.println("注册: "+succeedOrNot+" error: "+error);
		user.SendMessage(succeedOrNot, error, 8);
	}
	public static void BuildRoomReturn(User user,String succeedOrNot,String roomName) {
		System.out.println("buildRoom: "+roomName);
		user.SendMessage(succeedOrNot, roomName, 2);
	}
	
	public static void UserJoinRoomReturn(User user,String succeedOrNot,String userNameInRoom) {
		System.out.println("加入房间: "+succeedOrNot+" 房间中的用户： "+userNameInRoom);
		user.SendMessage(succeedOrNot, userNameInRoom, 3);
	}
	
	public static void UserSendMessageInRoomReturn(User user,String userName,String message) {
		user.SendMessage(userName, message, 4);
	}
	
	public static void UserLeaveRoomReturn(User user,String allNoLockRoomName,String allLockedRoomName) {
		System.out.println("LeaveRoom: "+allNoLockRoomName+" ||| "+allLockedRoomName);
		user.SendMessage(allNoLockRoomName, allLockedRoomName,5);
	}
	public static void FindAllRoomsReturn(User user,String allNoLockRoomName,String allLockedRoomName) {
		System.out.println("FindAllRoom: "+allNoLockRoomName+" ||| "+allLockedRoomName);
		user.SendMessage(allNoLockRoomName,allLockedRoomName, 7);
	}
	public static void GetAllFileInRoomReturn (User user,String filesName) {
		System.out.println("发送所有文件名： "+filesName);
		user.SendMessage(filesName, 10);
	}
	
	public static void UserSendFileReturn(User user,String succeedOrNot,String error) {
		if (error==null)error="";
		System.out.println("接受文件: "+succeedOrNot+" error: "+error);
		user.SendMessage(succeedOrNot, error, 9);
	}
	
	public static void GetFileInRoomReturn (User user,String path) {
		System.out.println("向客户端发送文件： "+path);
		FileInputStream fis;
		try {
			File file = new File(path);
			if(file.exists()) {
				if(file.isDirectory())return;
                fis = new FileInputStream(file);
                String fileName = file.getName();
                if(fileName.equals("")||fileName==null)return;
                long fileLength = file.length();
                user.SendMessage(fileName,fileLength, 11);
        		
        		byte[] bytes = new byte[2048];
                int length = 0;
                long progress = 0;
                while((length = fis.read(bytes, 0, bytes.length)) != -1) {
                	user.SendMessage(bytes,length);
                    
                    //打印传输的百分比
                    progress += length;
                    System.out.print("| " + (100*progress/file.length()) + "% |");
                }
                System.out.println(" ");
                fis.close();
            }else {
            	user.SendMessage("None",-1, 11);
			}
		}catch(Exception e) {}
	}
}
