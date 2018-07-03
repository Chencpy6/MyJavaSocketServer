import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SocketServer implements Runnable{
	private Thread t;
	private String threadName;
	private List<User> noRoomUser =  Collections.synchronizedList(new ArrayList<User>());
	private List<Room> Rooms =   Collections.synchronizedList(new ArrayList<Room>());
	
	public SocketServer() {
		this.threadName = "SocketServer";
	}
	public void start () {
	      System.out.println("Starting " +  threadName );
	      if (t == null) {
	         t = new Thread (this, threadName);
	         t.start ();
	      }
	}
	public void run() {
		try {
	         ServerSocket ss = new ServerSocket(8888);
	         System.out.println("启动服务器....");
	         System.out.println("服务器:"+ss.getInetAddress().getLocalHost()+"已ok");
	         
	         while(true) {
	        	 Socket c = ss.accept();
	        	 System.out.println("客户端:"+c.getInetAddress().getLocalHost()+"已连接到服务器");
	        	 User user = new User(c,this);
	        	 user.start();
	        	 noRoomUser.add(user);
	         }
	         
	      } catch (IOException e) {
	         //e.printStackTrace();
	    	  System.out.println(e);
	      }
		catch (Exception e) {
	    	  System.out.println(e);
	      }
		
		System.out.println("服务器：GG");
	}

	public void UserLogin(String userName,String password,User user) {
		if(password.equals("")) {
			SendTool.LoginReturn(user, "false", "Password is null!");
			return;
		}
		synchronized(Rooms) {
		if(Rooms.size()!=0) {
			for(int i=0;i<Rooms.size();i++) {
				Room temp_room = Rooms.get(i);
				if(temp_room.ContainUser(userName)) {
					SendTool.LoginReturn(user, "false", "User already exists!");
					return;
				}
			}
		}}
		synchronized(noRoomUser) {
		if(noRoomUser.size()!=0) {
			for(int i=0;i<noRoomUser.size();i++) {
				User u = noRoomUser.get(i);
				if(u.getUserName().equals(userName)) {
					SendTool.LoginReturn(user, "false", "User already exists!");
					return;
				}
			}
		}}
		user.setUserName(userName);
		SendTool.LoginReturn(user, "true", "");
	}
	
	public void UserRegister(String userName,String password,User user) {
		if(password.equals("")) {
			SendTool.LoginReturn(user, "false", "Password is null!");
			return;
		}
		SendTool.RegisterReturn(user, "true", "");
	}
	
	public void BuildRoom(String roomName,String password,User user) {
		synchronized(Rooms) {
		if(Rooms.size()!=0) {
			boolean repeat = false;
			{
				repeat = false;
				for(int i=0;i<Rooms.size();i++) {
					Room temp_room = Rooms.get(i);
					if(temp_room.getRoomName().equals(roomName)) {
						repeat = true;
						roomName += "S";
						break;
					}
				}
			}while(repeat);
		}
		Room t_room = new Room(user,roomName,password);
		t_room.AddUser(user);
		user.setRoom(t_room);
		noRoomUser.remove(user);
		Rooms.add(t_room);
		}
		//建个文件夹
		File file = new File(".\\Rooms\\"+roomName);
		if(!file.exists()) {
			file.mkdir();
        }
		SendTool.BuildRoomReturn(user, "true", roomName);
	}
	
	public void UserJoinRoom(String roomName,String password,User user) {
		Room temp_room = findRoomInList(roomName);
		if(temp_room==null) {
			SendTool.UserJoinRoomReturn(user, "false", "");
			return;
		}
		if(!password.equals(temp_room.getRoomPassword())) {
			SendTool.UserJoinRoomReturn(user, "false", "");
			return;
		}	
		temp_room.AddUser(user);
		noRoomUser.remove(user);
		user.setRoom(temp_room);
		SendTool.UserJoinRoomReturn(user, "true", temp_room.getAllUserName());
	}
	public void UserSendMessageInRoom(String message,User user) {
		List<User> users = user.getRoom().getRoomUser();
		String userName = user.getUserName();
		System.out.println("用户    "+user.getUserName()+"  发送的信息: "+message);
		for(int i=0;i<users.size();i++) {
			User t_User = users.get(i);
			if(!t_User.getUserName().equals(userName)) {
				System.out.println("发送到用户    "+t_User.getUserName()+"  发送的信息: "+message);
				SendTool.UserSendMessageInRoomReturn(t_User, userName, message);
			}
		}
	}
	public void UserLeaveRoom(User user) {
		noRoomUser.add(user);
		Room t_Room = user.getRoom();
		user.setRoom(null);
		List<User> t_Users = t_Room.getRoomUser();
		if(t_Users.size()<=1) {
			t_Room.RemoveUser(user);
			Rooms.remove(t_Room);
			
			DeleteRoomFile(t_Room.getRoomName());
		}else {
			t_Room.RemoveUser(user);
			if(user.getUserName().equals(t_Room.getRoomMaster().getUserName())) {
				t_Users = t_Room.getRoomUser();
				t_Room.setRoomMaster(t_Users.get(0));
			}
		}
		SendTool.UserLeaveRoomReturn(user, findAllUnLockRooms(), findAllLockedRooms());
	}
	
	public void FindAllRooms(User user) {
		SendTool.FindAllRoomsReturn(user, findAllUnLockRooms(), findAllLockedRooms());
	}
	
	public void GetAllFileInRoom(User user) {
		String filesName = "";
		String path = ".\\Rooms\\"+user.getRoom().getRoomName();
		File dir=new File(path);
        if(dir.exists()){
            File[] tmp=dir.listFiles();
            for(int i=0;i<tmp.length;i++){
            	filesName += tmp[i].getName();
            	filesName += ",";
            }
        }
		if(filesName.equals("")) {
			filesName = "None";
		}
		SendTool.GetAllFileInRoomReturn(user, filesName);
	}
	
	FileOutputStream fos;
	public byte[] UserSendFile(User user,DataInputStream in,String fileName,Long fileLength,byte[] b) {
		byte[] t_b = new byte[0];
		try {
		String roomName = user.getRoom().getRoomName();
		File directory = new File(".\\Rooms\\"+roomName);
        if(!directory.exists()) {
            directory.mkdir();
        }
        File file = new File(directory.getAbsolutePath() + File.separatorChar + fileName);
        fos = new FileOutputStream(file);
        // 开始接收文件
        byte[] bytes = new byte[2048];
        int length = 0;
        if(b.length>0) {
        	fos.write(b, 0, b.length);
            fos.flush();
            fileLength -= b.length;
        }
        
        while((length = in.read(bytes, 0, bytes.length)) != -1) {
        	//System.out.println("length值："+length);
        	if(fileLength>=length) {
        		fos.write(bytes, 0, length);
        		fos.flush();
        		fileLength -= length;
        		System.out.println("fileLength: "+fileLength);
        		if(fileLength<=0)break;
            }else {
            	fos.write(bytes, 0, fileLength.intValue());
        		fos.flush();
        		length -= fileLength.intValue();
        		System.out.println("fileLength: "+fileLength + " length-fileLength: "+length);
        		t_b = new byte[length];
        		System.arraycopy(bytes,fileLength.intValue(),t_b,0,t_b.length);
        		break;
			}
        }
		}catch(Exception e) {
			SendTool.UserSendFileReturn(user, "false", e.toString());
			System.out.println("出错了："+e.toString());
		}
		finally {
		 try {
             if(fos != null)
                 fos.close();
         } catch (Exception e) {}
		}
		SendTool.UserSendFileReturn(user, "true", "");
		return t_b;
	}
	//暂时没写
	public void ServerMessage(String serverMessage,User user) {
		
	}

	private Room findRoomInList(String roomName) {
		if(Rooms.size()==0) {
			return null;
		}
		for(int i=0;i<Rooms.size();i++) {
			Room t_room = Rooms.get(i);
			if(t_room.getRoomName().equals(roomName)) {
				return t_room; 
			}
		}
		return null;
	}
	
	private String findAllLockedRooms() {
		String roomNames = "";
		if(Rooms.size()!=0) {
			for(int i=0;i<Rooms.size();i++) {
				Room t_room = Rooms.get(i);
				if(!t_room.getRoomPassword().equals("")) {
					roomNames += t_room.getRoomName();
					roomNames += ",";
				}
			}
		}
		if(roomNames.equals("")) {
			roomNames = "None";
		}
		return roomNames;
	}
	
	private String findAllUnLockRooms() {
		String roomNames = "";
		if(Rooms.size()!=0) {
			for(int i=0;i<Rooms.size();i++) {
				Room t_room = Rooms.get(i);
				if(t_room.getRoomPassword().equals("")) {
					roomNames += t_room.getRoomName();
					roomNames += ",";
				}
			}
		}
		if(roomNames.equals("")) {
			roomNames = "None";
		}
		return roomNames;
	}
	public void DeleteRoomFile(String roomName) {
		File directory = new File(".\\Rooms\\"+roomName);
        if(directory.exists()) {
        	if (directory.isFile())
        		directory.delete();
            else
                delDir(directory.getAbsolutePath());
        }
	}
	 private void delDir(String path){
         File dir=new File(path);
         if(dir.exists()){
             File[] tmp=dir.listFiles();
             for(int i=0;i<tmp.length;i++){
                 if(tmp[i].isDirectory()){
                     delDir(path+"/"+tmp[i].getName());
                 }
                 else{
                     tmp[i].delete();
                 }
             }
             dir.delete();
         }
     }
	//getter and setter
	public List<User> getNoRoomUser() {
		return noRoomUser;
	}
	
	public List<Room> getRooms() {
		return Rooms;
	}
}
