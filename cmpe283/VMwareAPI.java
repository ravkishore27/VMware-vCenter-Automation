package cmpe283;
import java.util.Arrays;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import com.vmware.vim25.AlarmSetting;
import com.vmware.vim25.AlarmSpec;
import com.vmware.vim25.AlarmState;
import com.vmware.vim25.DuplicateName;
import com.vmware.vim25.HostVMotionCompatibility;
import com.vmware.vim25.HttpNfcLeaseDeviceUrl;
import com.vmware.vim25.HttpNfcLeaseInfo;
import com.vmware.vim25.HttpNfcLeaseState;
import com.vmware.vim25.OvfCreateDescriptorParams;
import com.vmware.vim25.OvfCreateDescriptorResult;
import com.vmware.vim25.OvfCreateImportSpecParams;
import com.vmware.vim25.OvfCreateImportSpecResult;
import com.vmware.vim25.OvfFile;
import com.vmware.vim25.OvfFileItem;
import com.vmware.vim25.OvfNetworkMapping;
import com.vmware.vim25.PerfEntityMetric;
import com.vmware.vim25.PerfEntityMetricBase;
import com.vmware.vim25.PerfMetricId;
import com.vmware.vim25.PerfMetricIntSeries;
import com.vmware.vim25.PerfMetricSeries;
import com.vmware.vim25.PerfProviderSummary;
import com.vmware.vim25.PerfQuerySpec;
import com.vmware.vim25.PerfSampleInfo;
import com.vmware.vim25.RuntimeFault;
import com.vmware.vim25.StateAlarmExpression;
import com.vmware.vim25.StateAlarmOperator;
import com.vmware.vim25.TaskInfo;
import com.vmware.vim25.VirtualDeviceConfigSpec;
import com.vmware.vim25.VirtualMachineCloneSpec;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VirtualMachineFileInfo;
import com.vmware.vim25.VirtualMachineMovePriority;
import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.VirtualMachineRelocateSpec;
import com.vmware.vim25.VirtualMachineRuntimeInfo;
import com.vmware.vim25.mo.Alarm;
import com.vmware.vim25.mo.AlarmManager;
import com.vmware.vim25.mo.ComputeResource;
import com.vmware.vim25.mo.Datacenter;
import com.vmware.vim25.mo.Datastore;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.HttpNfcLease;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.PerformanceManager;
import com.vmware.vim25.mo.ResourcePool;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualApp;
import com.vmware.vim25.mo.VirtualMachine;
import com.vmware.vim25.mo.VirtualMachineSnapshot;
import com.vmware.vim25.mo.samples.ovf.LeaseProgressUpdater;
import com.vmware.vim25.mo.samples.vm.CreateVM;
import com.vmware.vim25.mo.samples.vm.VMSnapshot;

/**
 * 
 * @author Ravi Kishore Angajala
 * @author Jay Mehta
 * @author Meet Dave
 */

public class VMwareAPI {

	private String vcenter = "https://192.168.1.44/sdk";
	private String uname = "root";
	private String pwd = "vmware";

	public String DatacenterName = "Datacenter";
	public ServiceInstance si;
	public Folder rootFolder;
	public Datacenter dc;
	public ManagedEntity[] resourcePool_list;
	public Datastore datastore;
	public ManagedEntity[] hosts;
	public ManagedEntity[] vms;
	public String vmName;
	public VirtualMachine vm;
	public HostSystem host;

	BufferedReader br = new BufferedReader(new InputStreamReader(System.in));


	//Initial Configuration
	VMwareAPI(){
		try {
			this.si = new ServiceInstance(new URL(vcenter), uname, pwd, true);
			rootFolder = si.getRootFolder();
			this.dc = (Datacenter) new InventoryNavigator(rootFolder).searchManagedEntity("Datacenter", this.DatacenterName);
			resourcePool_list = new InventoryNavigator(rootFolder).searchManagedEntities("ResourcePool");
			hosts = new InventoryNavigator(rootFolder).searchManagedEntities("HostSystem");
			vms = new InventoryNavigator(rootFolder).searchManagedEntities("VirtualMachine");
			datastore = dc.getDatastores()[1];
			//ManagedEntity[] ds = new InventoryNavigator(rootFolder).searchManagedEntities("Datastore");


		}
		catch(Exception e)
		{
			System.out.println(e);
		}
	}


	//1. Create a VM
	public Boolean createVM() throws Exception {
		System.out.println("Enter the name of the new VM");
		vmName = br.readLine();
		int len = dc.getDatastores().length;
		System.out.println("Please choose the number of the Host you want to create the VM");
		for(int i = 0; i < len; i++){
			System.out.println((i+1) + ". " + hosts[i].getName() + " " + datastore.getName());
		}

		int select = Integer.parseInt(br.readLine());

		long memorySizeMB = 500;
		int cupCount = 1;
		String guestOsId = "ubuntu64Guest";
		long diskSizeKB = 100000;
		// mode: persistent|independent_persistent,
		// independent_nonpersistent
		String diskMode = "persistent";
		String datastoreName = datastore.getName();

		String netName = "VM Network";
		String nicName = "Network Adapter 1";

		// create vm config spec
		VirtualMachineConfigSpec vmSpec = 
				new VirtualMachineConfigSpec();
		vmSpec.setName(vmName);
		vmSpec.setAnnotation("VirtualMachine Annotation");
		vmSpec.setMemoryMB(memorySizeMB);
		vmSpec.setNumCPUs(cupCount);
		vmSpec.setGuestId(guestOsId);

		// create virtual devices
		int cKey = 2;
		VirtualDeviceConfigSpec scsiSpec = CreateVM.createScsiSpec(cKey);
		VirtualDeviceConfigSpec diskSpec = CreateVM.createDiskSpec(
				datastoreName, cKey, diskSizeKB, diskMode);
		VirtualDeviceConfigSpec nicSpec = CreateVM.createNicSpec(
				netName, nicName);

		vmSpec.setDeviceChange(new VirtualDeviceConfigSpec[] 
				{scsiSpec, diskSpec, nicSpec});

		// create vm file info for the vmx file
		VirtualMachineFileInfo vmfi = new VirtualMachineFileInfo();
		vmfi.setVmPathName("["+ datastoreName +"]/" + vmName + "/" + vmName + ".vmx");
		vmSpec.setFiles(vmfi);

		System.out.println("VM creation is in process.. please wait ..");

		// call the createVM_Task method on the vm folder
		Task task = dc.getVmFolder().createVM_Task(vmSpec,(ResourcePool)resourcePool_list[0],(HostSystem) hosts[select - 1]);
		@SuppressWarnings("deprecation")
		String result = task.waitForMe();
		if(result == Task.SUCCESS)
		{
			System.out.println("VM Created Sucessfully");
			return true;
		}
		else 
		{
			System.out.println("VM could not be created. ");
			return false;
		}	    
	}

	//2. Remove VM
	public Boolean removeVM(String vmName) throws Exception {

		vm = (VirtualMachine) new InventoryNavigator(
				rootFolder).searchManagedEntity("VirtualMachine", vmName);
		if(vmName == null){
			System.out.println(vmName + " Not Found.");
			return false;
		}
		System.out.println("VM removal is in process.. please wait ..");
		Task task = vm.destroy_Task();
		@SuppressWarnings("deprecation")
		String result = task.waitForMe();
		if(result == Task.SUCCESS)
		{
			System.out.println( vmName + " VM removed successfully");
			return true;
		}
		else 
		{
			System.out.println( vmName + " VM was not removed successfully");
			return false;
		}	

	}

	//3. Rename VM
	public Boolean renameVM(String vmName, String renameVM) throws Exception {
		vm = (VirtualMachine) new InventoryNavigator(
				rootFolder).searchManagedEntity("VirtualMachine", vmName);
		if(vmName == null){
			System.out.println(vmName + " Not Found.");
			return false;
		}
		System.out.println("VM Renaming is in process.. please wait ..");
		Task task = vm.rename_Task(renameVM);
		@SuppressWarnings("deprecation")
		String result = task.waitForMe();

		if(result == Task.SUCCESS)
		{
			System.out.println("The name has been successfully changed.");
			return true;
		}
		else
		{
			System.out.println("The name cannot be changed.");
			return false;
		}
	}

	//4. Power On VM
	public Boolean powerOnVM(String vmName) throws Exception
	{
		vm = (VirtualMachine) new InventoryNavigator(
				rootFolder).searchManagedEntity("VirtualMachine", vmName);
		if(vmName == null){
			System.out.println(vmName + " Not Found.");
			return false;
		}

		VirtualMachineRuntimeInfo vmri = (VirtualMachineRuntimeInfo) vm.getRuntime();
		if(vmri.getPowerState() == VirtualMachinePowerState.poweredOff)
		{
			Task task = vm.powerOnVM_Task(null);
			@SuppressWarnings("deprecation")
			String result = task.waitForMe();
			if(result == Task.SUCCESS)
			{
				System.out.println("vm:" + vm.getName() + " powered on.");
				return true;
			}
			else
			{
				System.out.println("The VM couldn't be Powerd On");
				return false;
			}
		}
		else{
			System.out.println(vm.getName() + " is already PoweredOn");
			return true;
		}
	}

	//5. Power Off VM
	public boolean powerOffVM(String vmName) throws Exception
	{
		vm = (VirtualMachine) new InventoryNavigator(
				rootFolder).searchManagedEntity("VirtualMachine", vmName);
		if(vmName == null){
			System.out.println(vmName + " Not Found.");
			return false;
		}

		VirtualMachineRuntimeInfo vmri = (VirtualMachineRuntimeInfo) vm.getRuntime();
		if(vmri.getPowerState() == VirtualMachinePowerState.poweredOn)
		{
			Task task = vm.powerOffVM_Task();
			@SuppressWarnings("deprecation")
			String result = task.waitForMe();
			if(result == Task.SUCCESS)
			{
				System.out.println(vm.getName() + " powered off.");
				return true;
			}
			else
			{
				System.out.println("The VM couldn't be Powerd On");
				return false;
			}
		}
		else{
			System.out.println(vm.getName() + " is already PoweredOff");
			return true;
		}
	}

	//6. Ping VM
	public boolean pingVM(String vmName) throws Exception    {
		vm = (VirtualMachine) new InventoryNavigator(
				rootFolder).searchManagedEntity("VirtualMachine", vmName);
		if(vmName == null){
			System.out.println(vmName + " Not Found.");
			return false;
		}
		Boolean pinging=false;  
		String pingResult = "";
		String IpAddr = vm.getGuest().getIpAddress(); // Will only get IPAddress if the host has VMWare Tools
		if(IpAddr == null){
			System.out.println("VMware Tools are not installed on the Virtual Machine");
			return false;
		}
		String pingCmd = "ping " + IpAddr;

		Runtime r = Runtime.getRuntime();
		Process p = r.exec(pingCmd);		//Ping host ip executes from this line

		br = new BufferedReader(new
				InputStreamReader(p.getInputStream()));
		String inputLine;
		while ((inputLine = br.readLine()) != null) {
			pingResult += inputLine + "\n";
		}		
		br.close();
		if((pingResult.contains("(100% loss)")) || (pingResult.contains("timed out")) || (!(pingResult.contains("Reply from"))))
		{
			System.out.println("Destination unreachable");
			pinging=false;
		}
		else
		{
			System.out.println("Pinging on the host: "+ IpAddr);	
			pinging=true;
		}
		System.out.println(pingResult);
		return pinging;

	}

	//7. Ping all VMs
	public Boolean pingAllVMs() throws Exception    {
		Boolean pinging = false;
		for (int i=0; i < vms.length;i++){
			//System.out.println(vms[i].getName());
			VirtualMachine vMachine = (VirtualMachine) vms[i];
			pinging = false;
			pinging = pingVM(vMachine.getName());
			if (pinging == true){
				System.out.println(vMachine.getName()   +" :Ping success \n");
			}
			else {
				System.out.println(vMachine.getName() +" :Ping failure \n");
			}
		}
		return true;
	}


	//8. Get all VMs
	public ManagedEntity[] getVMs() throws IOException    {

		for(int i=0; i<vms.length; i++)
		{
			System.out.println(i + ". " + vms[i].getName());
		}
		return vms;
	}

	//9. Get all Hosts
	public ManagedEntity[] getHosts() throws IOException    {

		ManagedEntity[] hosts = new InventoryNavigator(rootFolder).searchManagedEntities(
				new String[][] { {"HostSystem", "name" }, }, true);

		for(int i=0; i<hosts.length; i++)
		{
			System.out.println( i + ". " + hosts[i].getName());
		}
		return hosts;
	}


	//10. Export OVF
	public static LeaseProgressUpdater leaseProgUpdater;
	public void ExportOvfToLocal(String vApporVmName, String hostip, String entityType, String targetDir) throws Exception
	{
		HostSystem host = (HostSystem) si.getSearchIndex().findByIp(null, hostip, false); 

		System.out.println("Host Name : " + host.getName());
		System.out.println("Network : " + host.getNetworks()[0].getName());
		System.out.println("Datastore : " + host.getDatastores()[0].getName());

		InventoryNavigator iv = new InventoryNavigator(si.getRootFolder());

		HttpNfcLease hnLease = null;

		ManagedEntity me = null;
		if (entityType.equals("VirtualApp"))
		{
			me = iv.searchManagedEntity("VirtualApp", vApporVmName);
			hnLease = ((VirtualApp)me).exportVApp();
		}
		else
		{
			me = iv.searchManagedEntity("VirtualMachine", vApporVmName);
			hnLease = ((VirtualMachine)me).exportVm();
		}

		// Wait until the HttpNfcLeaseState is ready
		HttpNfcLeaseState hls;
		for(;;)
		{
			hls = hnLease.getState();
			if(hls == HttpNfcLeaseState.ready)
			{
				break;
			}
			if(hls == HttpNfcLeaseState.error)
			{
				si.getServerConnection().logout();
				return;
			}
		}

		System.out.println("HttpNfcLeaseState: ready ");
		HttpNfcLeaseInfo httpNfcLeaseInfo = hnLease.getInfo();
		httpNfcLeaseInfo.setLeaseTimeout(300*1000*1000);
		printHttpNfcLeaseInfo1(httpNfcLeaseInfo);

		//Note: the diskCapacityInByte could be many time bigger than
		//the total size of VMDK files downloaded. 
		//As a result, the progress calculated could be much less than reality.
		long diskCapacityInByte = (httpNfcLeaseInfo.getTotalDiskCapacityInKB()) * 1024;

		leaseProgUpdater = new LeaseProgressUpdater(hnLease, 5000);
		leaseProgUpdater.start();

		long alredyWrittenBytes = 0;
		HttpNfcLeaseDeviceUrl[] deviceUrls = httpNfcLeaseInfo.getDeviceUrl();
		if (deviceUrls != null) 
		{
			OvfFile[] ovfFiles = new OvfFile[deviceUrls.length];
			System.out.println("Downloading Files:");
			for (int i = 0; i < deviceUrls.length; i++) 
			{
				String deviceId = deviceUrls[i].getKey();
				String deviceUrlStr = deviceUrls[i].getUrl();
				String diskFileName = deviceUrlStr.substring(deviceUrlStr.lastIndexOf("/") + 1);
				String diskUrlStr = deviceUrlStr.replace("*", hostip);
				String diskLocalPath = targetDir + diskFileName;
				System.out.println("File Name: " + diskFileName);
				System.out.println("VMDK URL: " + diskUrlStr);
				String cookie = si.getServerConnection().getVimService().getWsc().getCookie();
				long lengthOfDiskFile = writeVMDKFile(diskLocalPath, diskUrlStr, cookie, alredyWrittenBytes, diskCapacityInByte);
				alredyWrittenBytes += lengthOfDiskFile;
				OvfFile ovfFile = new OvfFile();
				ovfFile.setPath(diskFileName);
				ovfFile.setDeviceId(deviceId);
				ovfFile.setSize(lengthOfDiskFile);
				ovfFiles[i] = ovfFile;
			}

			OvfCreateDescriptorParams ovfDescParams = new OvfCreateDescriptorParams();
			ovfDescParams.setOvfFiles(ovfFiles);
			OvfCreateDescriptorResult ovfCreateDescriptorResult = 
					si.getOvfManager().createDescriptor(me, ovfDescParams);

			String ovfPath = targetDir + vApporVmName + ".ovf";
			FileWriter out = new FileWriter(ovfPath);
			out.write(ovfCreateDescriptorResult.getOvfDescriptor());
			out.close();
			System.out.println("OVF Desriptor Written to file: " + ovfPath);
		} 

		System.out.println("Completed Downloading the files");
		leaseProgUpdater.interrupt();
		hnLease.httpNfcLeaseProgress(100);
		hnLease.httpNfcLeaseComplete();

		si.getServerConnection().logout();
	}


	private static void printHttpNfcLeaseInfo1(HttpNfcLeaseInfo info) 
	{
		System.out.println("########################  HttpNfcLeaseInfo  ###########################");
		System.out.println("Lease Timeout: " + info.getLeaseTimeout());
		System.out.println("Total Disk capacity: "	+ info.getTotalDiskCapacityInKB());
		HttpNfcLeaseDeviceUrl[] deviceUrlArr = info.getDeviceUrl();
		if (deviceUrlArr != null) 
		{
			int deviceUrlCount = 1;
			for (HttpNfcLeaseDeviceUrl durl : deviceUrlArr) 
			{
				System.out.println("HttpNfcLeaseDeviceUrl : "
						+ deviceUrlCount++);
				System.out.println("	Device URL Import Key: "
						+ durl.getImportKey());
				System.out.println("	Device URL Key: " + durl.getKey());
				System.out.println("	Device URL : " + durl.getUrl());
				System.out.println("	SSL Thumbprint : "	+ durl.getSslThumbprint());
			}
		} 
		else
		{
			System.out.println("No Device URLS Found");
		}
	}

	private static long writeVMDKFile(String localFilePath, String diskUrl, String cookie, 
			long bytesAlreadyWritten, long totalBytes) throws IOException 
			{
		HttpsURLConnection conn = getHTTPConnection(diskUrl, cookie);
		InputStream in = conn.getInputStream();
		OutputStream out = new FileOutputStream(new File(localFilePath));
		byte[] buf = new byte[102400];
		int len = 0;
		long bytesWritten = 0;
		while ((len = in.read(buf)) > 0) 
		{
			out.write(buf, 0, len);
			bytesWritten += len;
			int percent = (int)(((bytesAlreadyWritten + bytesWritten) * 100) / totalBytes);
			leaseProgUpdater.setPercent(percent);
			System.out.println("written: " + bytesWritten);
		}
		in.close();
		out.close();
		return bytesWritten;
			}

	private static HttpsURLConnection getHTTPConnection(String urlStr, String cookieStr) throws IOException 
	{
		HostnameVerifier hv = new HostnameVerifier() 
		{
			public boolean verify(String urlHostName, SSLSession session) 
			{
				return true;
			}
		};
		HttpsURLConnection.setDefaultHostnameVerifier(hv);
		URL url = new URL(urlStr);
		HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

		conn.setDoInput(true);
		conn.setDoOutput(true);
		conn.setAllowUserInteraction(true);
		conn.setRequestProperty("Cookie",	cookieStr);
		conn.connect();
		return conn;
	}

	//11. Import OVF
	private static final int CHUCK_LEN = 64 * 1024;
	public static LeaseProgressUpdater leaseUpdater;
	public void ImportLocalOvfVApp(String ovfLocal, String hostipadd, String newVmName) throws Exception 
	{
		HostSystem host = (HostSystem) si.getSearchIndex().findByIp(null, hostipadd, false); 

		System.out.println("Host Name : " + host.getName());
		System.out.println("Network : " + host.getNetworks()[0].getName());
		System.out.println("Datastore : " + host.getDatastores()[0].getName());

		Folder vmFolder = (Folder) host.getVms()[0].getParent();

		OvfCreateImportSpecParams importSpecParams = new OvfCreateImportSpecParams();
		importSpecParams.setHostSystem(host.getMOR());
		importSpecParams.setLocale("US");
		importSpecParams.setEntityName(newVmName);
		importSpecParams.setDeploymentOption("");
		OvfNetworkMapping networkMapping = new OvfNetworkMapping();
		networkMapping.setName("Network 1");
		networkMapping.setNetwork(host.getNetworks()[0].getMOR()); // network);
		importSpecParams.setNetworkMapping(new OvfNetworkMapping[] { networkMapping });
		importSpecParams.setPropertyMapping(null);

		String ovfDescriptor = readOvfContent(ovfLocal);
		if (ovfDescriptor == null) 
		{
			si.getServerConnection().logout();
			return;
		}

		ovfDescriptor = escapeSpecialChars(ovfDescriptor);
		System.out.println("ovfDesc:" + ovfDescriptor);

		ResourcePool rp = ((ComputeResource)host.getParent()).getResourcePool();

		OvfCreateImportSpecResult ovfImportResult = si.getOvfManager().createImportSpec(
				ovfDescriptor, rp, host.getDatastores()[0], importSpecParams);

		if(ovfImportResult==null)
		{
			si.getServerConnection().logout();
			return;
		}

		long totalBytes = addTotalBytes(ovfImportResult);
		System.out.println("Total bytes: " + totalBytes);

		HttpNfcLease httpNfcLease = null;

		httpNfcLease = rp.importVApp(ovfImportResult.getImportSpec(), vmFolder, host);

		// Wait until the HttpNfcLeaseState is ready
		HttpNfcLeaseState hls;
		for(;;)
		{
			hls = httpNfcLease.getState();
			if(hls == HttpNfcLeaseState.ready || hls == HttpNfcLeaseState.error)
			{
				break;
			}
		}

		if (hls.equals(HttpNfcLeaseState.ready)) 
		{
			System.out.println("HttpNfcLeaseState: ready ");
			HttpNfcLeaseInfo httpNfcLeaseInfo = (HttpNfcLeaseInfo) httpNfcLease.getInfo();
			printHttpNfcLeaseInfo1(httpNfcLeaseInfo);

			leaseUpdater = new LeaseProgressUpdater(httpNfcLease, 5000);
			leaseUpdater.start();

			HttpNfcLeaseDeviceUrl[] deviceUrls = httpNfcLeaseInfo.getDeviceUrl();

			long bytesAlreadyWritten = 0;
			for (HttpNfcLeaseDeviceUrl deviceUrl : deviceUrls) 
			{
				String deviceKey = deviceUrl.getImportKey();
				for (OvfFileItem ovfFileItem : ovfImportResult.getFileItem()) 
				{
					if (deviceKey.equals(ovfFileItem.getDeviceId())) 
					{
						System.out.println("Import key==OvfFileItem device id: " + deviceKey);
						String absoluteFile = new File(ovfLocal).getParent() + File.separator + ovfFileItem.getPath();
						String urlToPost = deviceUrl.getUrl().replace("*", hostipadd);
						uploadVmdkFile(ovfFileItem.isCreate(), absoluteFile, urlToPost, bytesAlreadyWritten, totalBytes);
						bytesAlreadyWritten += ovfFileItem.getSize();
						System.out.println("Completed uploading the VMDK file:" + absoluteFile);
					}
				}
			}

			leaseUpdater.interrupt();
			httpNfcLease.httpNfcLeaseProgress(100);
			httpNfcLease.httpNfcLeaseComplete();
		}
		si.getServerConnection().logout();
	}


	public static long addTotalBytes(OvfCreateImportSpecResult ovfImportResult)
	{
		OvfFileItem[] fileItemArr = ovfImportResult.getFileItem();

		long totalBytes = 0;
		if (fileItemArr != null) 
		{
			for (OvfFileItem fi : fileItemArr) 
			{
				printOvfFileItem(fi);
				totalBytes += fi.getSize();
			}
		}
		return totalBytes;
	}

	private static void uploadVmdkFile(boolean put, String diskFilePath, String urlStr, 
			long bytesAlreadyWritten, long totalBytes) throws IOException 
			{
		HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() 
		{
			public boolean verify(String urlHostName, SSLSession session) 
			{
				return true;
			}
		});

		HttpsURLConnection conn = (HttpsURLConnection) new URL(urlStr).openConnection();
		conn.setDoOutput(true);
		conn.setUseCaches(false);
		conn.setChunkedStreamingMode(CHUCK_LEN);
		conn.setRequestMethod(put? "PUT" : "POST"); // Use a post method to write the file.
		conn.setRequestProperty("Connection", "Keep-Alive");
		conn.setRequestProperty("Content-Type",	"application/x-vnd.vmware-streamVmdk");
		conn.setRequestProperty("Content-Length", Long.toString(new File(diskFilePath).length()));

		BufferedOutputStream bos = new BufferedOutputStream(conn.getOutputStream());

		BufferedInputStream diskis = new BufferedInputStream(new FileInputStream(diskFilePath));
		int bytesAvailable = diskis.available();
		int bufferSize = Math.min(bytesAvailable, CHUCK_LEN);
		byte[] buffer = new byte[bufferSize];

		long totalBytesWritten = 0;
		while (true) 
		{
			int bytesRead = diskis.read(buffer, 0, bufferSize);
			if (bytesRead == -1) 
			{
				System.out.println("Total bytes written: " + totalBytesWritten);
				break;
			}

			totalBytesWritten += bytesRead;
			bos.write(buffer, 0, bufferSize);
			bos.flush();
			System.out.println("Total bytes written: " + totalBytesWritten);
			int progressPercent = (int) (((bytesAlreadyWritten + totalBytesWritten) * 100) / totalBytes);
			leaseUpdater.setPercent(progressPercent);			
		}

		diskis.close();
		bos.flush();
		bos.close();
		conn.disconnect();
			}

	public static String readOvfContent(String ovfFilePath)	throws IOException 
	{
		StringBuffer strContent = new StringBuffer();
		BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(ovfFilePath)));
		String lineStr;
		while ((lineStr = in.readLine()) != null) 
		{
			strContent.append(lineStr);
		}
		in.close();
		return strContent.toString();
	}

	private static void printHttpNfcLeaseInfo(HttpNfcLeaseInfo info) 
	{
		System.out.println("================ HttpNfcLeaseInfo ================");
		HttpNfcLeaseDeviceUrl[] deviceUrlArr = info.getDeviceUrl();
		for (HttpNfcLeaseDeviceUrl durl : deviceUrlArr) 
		{
			System.out.println("Device URL Import Key: " + durl.getImportKey());
			System.out.println("Device URL Key: " + durl.getKey());
			System.out.println("Device URL : " + durl.getUrl());
			System.out.println("Updated device URL: " + durl.getUrl());
		}
		System.out.println("Lease Timeout: " + info.getLeaseTimeout());
		System.out.println("Total Disk capacity: " + info.getTotalDiskCapacityInKB());
		System.out.println("==================================================");
	}

	private static void printOvfFileItem(OvfFileItem fi) 
	{
		System.out.println("================ OvfFileItem ================");
		System.out.println("chunkSize: " + fi.getChunkSize());
		System.out.println("create: " + fi.isCreate());
		System.out.println("deviceId: " + fi.getDeviceId());
		System.out.println("path: " + fi.getPath());
		System.out.println("size: " + fi.getSize());
		System.out.println("==============================================");
	}

	public static String escapeSpecialChars(String str)
	{
		str = str.replaceAll("<", "&lt;");
		return str.replaceAll(">", "&gt;"); // do not escape "&" -> "&amp;", "\"" -> "&quot;"
	}



	//12. create snapshot
	@SuppressWarnings("deprecation")
	public void createSnVM(String vmName, String snapshotName, String desc) throws RemoteException, MalformedURLException {

		vm = (VirtualMachine) new InventoryNavigator(
				rootFolder).searchManagedEntity("VirtualMachine", vmName);
		if(vmName == null){
			System.out.println(vmName + " Not Found.");
			return;
		}
		Task task = vm.createSnapshot_Task(
				snapshotName, desc, false, false);
		if(task.waitForMe()==Task.SUCCESS)
		{
			System.out.println("Snapshot was created.");
		}
		else
		{
			System.out.println("Unable to create snapshot.");  
		}
	}

	//13. Remove one snapshot
	@SuppressWarnings("deprecation")
	public void removeSnVm(String vmName, String snapshotName) throws Exception{
		vm = (VirtualMachine) new InventoryNavigator(
				rootFolder).searchManagedEntity("VirtualMachine", vmName);
		VirtualMachineSnapshot vmsnap = VMSnapshot.getSnapshotInTree(vm,snapshotName);
		if (vmsnap != null) {
			Task task = vmsnap.removeSnapshot_Task(true);
			if(task.waitForMe()==Task.SUCCESS)
			{
				System.out.println(snapshotName + " Snapshot removed");
			}
			else
			{
				System.out.println("Unable to remove snapshot.");  
			}
		}
		else
		{
			System.out.println("Snapshot not Found");
		}

	}

	//14. revert from snapshot VM
	@SuppressWarnings("deprecation")
	public Boolean revertSnVM(String vmName, String revert_snapshotName) throws IOException {
		vm = (VirtualMachine) new InventoryNavigator(
				rootFolder).searchManagedEntity("VirtualMachine", vmName);
		VirtualMachineSnapshot vmsnap = VMSnapshot.getSnapshotInTree(vm, revert_snapshotName);
		Task task = vmsnap.revertToSnapshot_Task(null);
		if(task.waitForMe()==Task.SUCCESS)
		{
			System.out.println("Reverted to " + revert_snapshotName + " Snapshot");
			return true;
		}
		else
		{
			System.out.println("Unable to create snapshot.");  
			return false;
		}
	}

	//15. List all snapshot
	public void listSnVm(String vmName) throws Exception{
		vm = (VirtualMachine) new InventoryNavigator(
				rootFolder).searchManagedEntity("VirtualMachine", vmName);
		VMSnapshot.listSnapshots(vm);

	}

	//16. Remove all snapshots
	@SuppressWarnings("deprecation")
	public void removeAllSnVM(String vmName) throws Exception{
		vm = (VirtualMachine) new InventoryNavigator(
				rootFolder).searchManagedEntity("VirtualMachine", vmName);
		Task task = vm.removeAllSnapshots_Task();
		if(task.waitForMe()==Task.SUCCESS)
		{
			System.out.println("removed all snapshots of " + vmName );
		}
		else
		{
			System.out.println("Unable to create snapshot.");  
		}
	}

	//17. Clone VM 
	public void cloneVM(String vmName,String cloneName) throws Exception{
		vm = (VirtualMachine) new InventoryNavigator(
				rootFolder).searchManagedEntity(
						"VirtualMachine", vmName);

		if(vm==null)
		{
			System.out.println("No VM " + vmName + " found");
			return;
		}

		if(vm.getRuntime().getPowerState().toString() == "poweredOn"){
			System.out.println("The VM cannot be in  poweredOn State");
			return;
		}

		/*
		 * Expansion for cloning onto other hosts using shared datastore
		System.out.println("Enter the Host you to want to Clone to");

		for(int i = 0; i < hosts.length; i++){
			System.out.println((i+1) + ". " + hosts[i].getName() + " " + datastore.getName());
		}

		int select = Integer.parseInt(br.readLine());*/

		VirtualMachineCloneSpec cloneSpec = 
				new VirtualMachineCloneSpec();
		cloneSpec.setLocation(new VirtualMachineRelocateSpec());
		cloneSpec.setPowerOn(false);
		cloneSpec.setTemplate(false);

		Task task = vm.cloneVM_Task((Folder) vm.getParent(), 
				cloneName, cloneSpec);
		System.out.println("Launching the VM clone task. " +
				"Please wait ...");

		@SuppressWarnings("deprecation")
		String status = task.waitForMe();
		if(status==Task.SUCCESS)
		{
			System.out.println("VM got cloned successfully.");
		}
		else
		{
			System.out.println("Failure -: VM cannot be cloned");
		}
	}

	
	//18. Migrate VM
	@SuppressWarnings("deprecation")
	public void migrateVM(String vmName, String hostIp) throws Exception{
		String newHostName = hostIp;

		VirtualMachine vm = (VirtualMachine) new InventoryNavigator(
				rootFolder).searchManagedEntity(
						"VirtualMachine", vmName);
		HostSystem newHost = (HostSystem) new InventoryNavigator(
				rootFolder).searchManagedEntity(
						"HostSystem", newHostName);
		System.out.println(newHost);
		ComputeResource cr = (ComputeResource) newHost.getParent();

		String[] checks = new String[] {"cpu", "software"};
		HostVMotionCompatibility[] vmcs =
				si.queryVMotionCompatibility(vm, new HostSystem[] 
						{newHost},checks );

		String[] comps = vmcs[0].getCompatibility();
		if(checks.length != comps.length)
		{
			System.out.println("CPU/software NOT compatible. Exit.");
			si.getServerConnection().logout();
			return;
		}
		System.out.println(cr.getResourcePool().getParent().getName());
		Task task = vm.migrateVM_Task(cr.getResourcePool(), newHost,
				VirtualMachineMovePriority.highPriority, 
				VirtualMachinePowerState.poweredOn);

		if(task.waitForMe()==Task.SUCCESS)
		{
			System.out.println("VMotioned!");
		}
		else
		{
			System.out.println("VMotion failed!");
			TaskInfo info = task.getTaskInfo();
			System.out.println(info.getError().getFault());
		}
		si.getServerConnection().logout();
	}
	
	
	//19. Disaster Recovery
	public void disasterRecovery() throws Exception{
		boolean pinging=false;
		boolean isSetAlarm = false;

		while(true){
			try {
				pinging=isSetAlarm=false;


				for (int j=0;j<vms.length;j++){
					vm = (VirtualMachine) vms[j];
					if((vm.getSummary().runtime.powerState.toString().equals("poweredOn")) && vm.getGuest().toolsRunningStatus.equals("guestToolsRunning")){

						System.out.println(vm.getName());
						/*Remove alarms*/
						removeAlarm(vm.getName()); 

						/*Create Alarm Definitions*/
						createAlarm(vm.getName());

						/*Check alarm status*/
						isSetAlarm = checkAlarm(vm.getName());

						
						
						/* Ping the VMs*/
						if ((isSetAlarm != true)){

							pinging = pingVM(vm.getName());

							if (pinging == true){
								System.out.println(vm.getName()+" is up with | CPU : "+ vm.getSummary().quickStats.overallCpuUsage+" |Memory : "+vm.getSummary().quickStats.getPrivateMemory()+" MB |");
								System.out.println(vm.getSnapshot());
								if(vm.getSnapshot() == null){
									System.out.println("Creating the first snapshot");
									createSnVM(vm.getName(), "Recent", "Creating every few minutes");
								}else{
									removeSnVm(vm.getName(), "Recent");
									System.out.println("removing and creating a new snapshot");
									createSnVM(vm.getName(), "Recent", "Creating every few minutes");
								}

							}
							else {
								System.out.println(vm.getName()+" is not pinging");
								System.out.println("Recovering the VM "+ vm.getName()+" from its snapshot...");
								System.out.println(vm.getSnapshot().getCurrentSnapshot().toString());
								int len = vm.getSnapshot().getRootSnapshotList().length;
								revertSnVM(vm.getName(),vm.getSnapshot().getRootSnapshotList()[len-1].getName());
								powerOnVM(vm.getName());
								/*
							 	if (PingTest(vm.getName())){
										System.out.println(vm.getName()+" is pinging now.");
								}else {
										System.out.println(vm.getName()+" is not pinging even after snapshot reversion.");
								}*/

							}

						}

					}

				}

			}catch (Exception e) {
				System.out.println("Exception occured " + Arrays.toString(e.getStackTrace()));
			}
		}
	}

	//20. Load Balancing
	public void loadBalancing() throws Exception{

		try {
			double lowCpuUsage = getHostCPUUsage(hosts[0].getName());
			double highCpuUsage = getHostCPUUsage(hosts[0].getName());
			HostSystem lowCpuUsageHost = (HostSystem) hosts[0];
			HostSystem highCpuUsageHost = (HostSystem) hosts[0];

			for(int i=1; i< hosts.length; i++){
				if(getHostCPUUsage(hosts[i].getName())<=lowCpuUsage){
					lowCpuUsage=getHostCPUUsage(hosts[i].getName());
					lowCpuUsageHost=(HostSystem) hosts[i];
				}
				if(getHostCPUUsage(hosts[i].getName())>=highCpuUsage){
					highCpuUsage = getHostCPUUsage(hosts[i].getName());
					highCpuUsageHost = (HostSystem) hosts[i];	
				}
			}

			System.out.println("Lowest:  Host System : " + lowCpuUsageHost.getName() + " with " + lowCpuUsage + "MHz" );
			System.out.println("Highest:  Host System : " + highCpuUsageHost.getName() + "With " + highCpuUsage + "MHz");
			if (lowCpuUsage < 1000){
				double avgVM=0.0;
				double total=0.0;
				int number=0;

				for (VirtualMachine v : highCpuUsageHost.getVms())
				{
					if (v.getRuntime().getPowerState().toString()=="poweredOn")
					{
						System.out.println(v.getName());
						total=total + getVMCPUUsage(v.getName());
						number++;
					}
				}

				System.out.println("Number of Machines running = : " + number);
				avgVM=total/number;
				System.out.println("Average of vms: "+avgVM);

				for (VirtualMachine v : highCpuUsageHost.getVms())

				{	
					if (v.getRuntime().getPowerState().toString()=="poweredOn")
					{
						if( getVMCPUUsage(v.getName())<avgVM){
							System.out.println(v.getName()+":"+getVMCPUUsage(v.getName()));
							migrateVM(v.getName(), lowCpuUsageHost.getName());
						}
					}
				}
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}

	}



	//21. Create Alarm
	public void createAlarm(String vmName) throws DuplicateName, RuntimeFault, RemoteException {
		ManagedEntity me =  new InventoryNavigator(
				rootFolder).searchManagedEntity(
						"VirtualMachine", vmName);
		AlarmManager alarmMgr = si.getAlarmManager();
		//System.out.println(alarmMgr.getAlarmState(me));
		AlarmSpec Spec = new AlarmSpec();

		StateAlarmExpression expression  = createStateAlarmExpression();


		Spec.setExpression(expression);
		Spec.setName("VmPowerStateAlarm"+me.getName());
		Spec.setDescription("Alarm by Team12");
		Spec.setEnabled(true);

		AlarmSetting as = new AlarmSetting();
		as.setReportingFrequency(0); 
		as.setToleranceRange(0);

		Spec.setSetting(as);

		alarmMgr.createAlarm(me, Spec);

	}

	static StateAlarmExpression createStateAlarmExpression() {
		StateAlarmExpression expression
		= new StateAlarmExpression();
		expression.setType("VirtualMachine");
		expression.setStatePath("runtime.powerState");
		expression.setOperator(StateAlarmOperator.isEqual);
		expression.setRed("poweredOff");
		return expression;
	}

	
	//22. Check Alarm
	public boolean checkAlarm(String vmName) throws RemoteException {
		vm = (VirtualMachine) new InventoryNavigator(
				rootFolder).searchManagedEntity(
						"VirtualMachine", vmName);
		boolean isSetAlarm = false;
		AlarmState[] alarmStates=vm.getTriggeredAlarmState();
		if (alarmStates != null){
			//System.out.println(alarmStates[0].key);
			isSetAlarm = true;
		}
		return isSetAlarm;
	}

	//23. Remove Alarm
	public void removeAlarm(String Name) throws RemoteException {
		ManagedEntity me =  new InventoryNavigator(
				rootFolder).searchManagedEntity(
						"VirtualMachine", vmName);
		Alarm[] alarm = si.getAlarmManager().getAlarm(me);
		for (Alarm alarm1 : alarm) {
			alarm1.removeAlarm();
		}
	}

	//24. Get VM CPU Usage
	public Double getVMCPUUsage(String VMName) throws Exception{

		vm = (VirtualMachine) new InventoryNavigator(
				si.getRootFolder()).searchManagedEntity(
						"VirtualMachine", VMName);
		PerformanceManager perfMgr = si.getPerformanceManager();
		PerfProviderSummary summary = perfMgr
				.queryPerfProviderSummary(vm);
		int perfInterval = summary.getRefreshRate();
		PerfMetricId[] queryAvailablePerfMetric = perfMgr
				.queryAvailablePerfMetric(vm, null, null,
						perfInterval);


		PerfQuerySpec qSpec = new PerfQuerySpec();
		qSpec.setEntity(vm.getMOR());



		qSpec.setMaxSample(1);
		qSpec.setMetricId(queryAvailablePerfMetric);

		qSpec.intervalId = perfInterval;
		PerfEntityMetricBase[] pembs = perfMgr
				.queryPerf(new PerfQuerySpec[] { qSpec });


		for (int i = 0; pembs != null && i < pembs.length; i++) {

			PerfEntityMetricBase val = pembs[i];
			PerfEntityMetric pem = (PerfEntityMetric) val;
			PerfMetricSeries[] vals = pem.getValue();
			PerfSampleInfo[] infos = pem.getSampleInfo();

			for (int j = 0; vals != null && j < vals.length; ++j) {
				PerfMetricIntSeries val1 = (PerfMetricIntSeries) vals[j];


				long[] longs = val1.getValue();

				if (val1.getId().getCounterId() == 6)
					return new Double(longs[0]);


			}
		}

		return null;

	}


	//25. Get HOST CPU Usage
	public Double getHostCPUUsage(String HostName) throws Exception{

		host = (HostSystem) new InventoryNavigator(si.getRootFolder()).searchManagedEntity("HostSystem", HostName);

		PerformanceManager perfMgr = si.getPerformanceManager();
		PerfProviderSummary summary = perfMgr
				.queryPerfProviderSummary(host);
		int perfInterval = summary.getRefreshRate();
		PerfMetricId[] queryAvailablePerfMetric = perfMgr
				.queryAvailablePerfMetric(host, null, null,
						perfInterval);

		PerfQuerySpec qSpec = new PerfQuerySpec();
		qSpec.setEntity(host.getMOR());

		qSpec.setMaxSample(1);
		qSpec.setMetricId(queryAvailablePerfMetric);

		qSpec.intervalId = perfInterval;
		PerfEntityMetricBase[] pembs = perfMgr
				.queryPerf(new PerfQuerySpec[] { qSpec });

		for (int i = 0; pembs != null && i < pembs.length; i++) {

			PerfEntityMetricBase val = pembs[i];
			PerfEntityMetric pem = (PerfEntityMetric) val;
			PerfMetricSeries[] vals = pem.getValue();
			PerfSampleInfo[] infos = pem.getSampleInfo();

			for (int j = 0; vals != null && j < vals.length; ++j) {
				PerfMetricIntSeries val1 = (PerfMetricIntSeries) vals[j];

				long[] longs = val1.getValue();

				if (val1.getId().getCounterId() == 6)
					return new Double(longs[0]);
			}
		}
		return null;
	}



	

}
