package cmpe283;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * 
 * @author RaviKishore Angajala
 * @author Jay Mehta
 * @author Meet Dave
 *
 */
public class MainController {

	public static void main(String[] args) throws Exception {

		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		VMwareAPI server = new VMwareAPI();
		int choice;
		String vmName;

		while(true)
		{
			System.out.println("Please select one of the following choices");
			System.out.println("1. Create VM");
			System.out.println("2. Remove VM");
			System.out.println("3. Rename VM");
			System.out.println("4. Power On VM");
			System.out.println("5. Power Off VM");
			System.out.println("6. Ping VM");
			System.out.println("7. Ping All VMs");
			System.out.println("8. Get VMs");
			System.out.println("9. Get Hosts");
			System.out.println("10. Export OVF");
			System.out.println("11. Import OVF");
			System.out.println("12. Create Snapshot");
			System.out.println("13. Remove Snapshot");
			System.out.println("14. Revert Snapshot");
			System.out.println("15. List Snapshots");
			System.out.println("16. Remove All Snapshots");
			System.out.println("17. Clone VM");
			System.out.println("18. Migrate VM");
			System.out.println("19. Disaster Recovery");
			System.out.println("20. Load Balancing");

			choice = Integer.parseInt(br.readLine());


			//Check API
			try
			{
				switch (choice) {

				//Create a VM
				case 1:
					server.createVM(); //Not inserting disk
					break;

					//Remove a VM
				case 2:
					System.out.println("Enter the VM name to Remove");
					vmName = br.readLine();
					server.removeVM(vmName);
					break;

					//Rename VM
				case 3:
					System.out.println("Enter the VM name to Rename");
					vmName = br.readLine();
					System.out.println("Enter the new Name");
					String renameVM = br.readLine();
					server.renameVM(vmName,renameVM);
					break;

					//PowerOn
				case 4:
					System.out.println("Enter the VM name to PowerOn");
					vmName = br.readLine();
					server.powerOnVM(vmName);
					break;

					//Power Off
				case 5:
					System.out.println("Enter the VM name to Poweroff");
					vmName = br.readLine();
					server.powerOffVM(vmName);
					break;

					//Ping VM
				case 6:
					System.out.println("Enter the VM name to ping");
					vmName = br.readLine();
					server.pingVM(vmName);
					break;

					//PingAllVMs
				case 7:
					server.pingAllVMs();
					break;

					//Get all VMs
				case 8:
					server.getVMs();
					break;

					//Get all Hosts
				case 9:
					server.getHosts();
					break;

					//Export OVF	
				case 10:
					System.out.println("Enter the VM name to export as OVF");
					String vApporVmName = br.readLine();
					System.out.println("Enter the hostip");
					String hostip = br.readLine();
					System.out.println("Enter the entityType");
					String entityType = br.readLine();
					System.out.println("Enter the Target Directory");
					String targetDir = br.readLine();
					server.ExportOvfToLocal(vApporVmName, hostip, entityType, targetDir);
					break;

					//Import OVF
				case 11:
					System.out.println("Enter the OVF file to be imported");
					String ovfLocal = br.readLine();
					System.out.println("Enter the hostip");
					String hostipadd = br.readLine();
					System.out.println("Enter the New Virtual Machine Name");
					String newVmName = br.readLine();
					server.ImportLocalOvfVApp(ovfLocal, hostipadd, newVmName);
					break;

				//Create Snapshot
				case 12:
					System.out.println("Enter the VM name to create Snapshot");
					vmName = br.readLine();
					System.out.println("Enter the Snapshot name");
					String snapshotName = br.readLine();
					System.out.println("Enter the Snapshot descripton");
					String desc = br.readLine();
					server.createSnVM(vmName, snapshotName, desc);
					break;
					
				//Remove Snapshot
				case 13:
					System.out.println("Enter the VM name to remove to Snapshot");
					vmName = br.readLine();
					System.out.println("Enter the Snapshot to be removed");
					snapshotName = br.readLine();
					server.removeSnVm( vmName,  snapshotName);
					break;

					//Revert to Snapshot
				case 14:
					System.out.println("Enter the VM name to revert to Snapshot");
					vmName = br.readLine();
					System.out.println("Enter the Snapshot name to revert to VM");
					String revert_snapshotName = br.readLine();
					server.revertSnVM(vmName, revert_snapshotName);
					break;

					//List all Snapshots
				case 15:
					System.out.println("Enter the VM name to list the Snapshots");
					vmName = br.readLine();
					server.listSnVm(vmName);
					break;

				

					//Remove all snapshots
				case 16:
					System.out.println("Enter the VM name to remove all the Snapshots");
					vmName = br.readLine();
					server.removeAllSnVM(vmName);
					break;

					//Clone a VM
				case 17:
					System.out.println("Enter the VM name to clone");
					vmName = br.readLine();
					System.out.println("Enter the Clone VM name");
					String cloneName = br.readLine();
					server.cloneVM(vmName, cloneName);
					break;

					//Migration
				case 18:
					System.out.println("Enter the VM name to Migrate");
					vmName = br.readLine();
					System.out.println("Enter the HostIp");
					String hostIp = br.readLine();
					server.migrateVM(vmName, hostIp );
					break;

					//Disaster Recovery	
				case 19:
					System.out.println("Starting Disaster Recovery");
					server.disasterRecovery();
					break;
					
					
					//Load Balancing	
				case 20:
					System.out.println("Starting Load Balancing");
					server.loadBalancing();
					break;
					
					
					//Create Alarm
				case 21:
					System.out.println("Enter the VM name to create Alarm");
					vmName = br.readLine();
					server.createAlarm(vmName);
					break;


					//Check Alarm	
				case 22:
					System.out.println("Enter the VM name to check Alarm");
					vmName = br.readLine();
					server.checkAlarm(vmName);
					break;

					//remove Alarm	
				case 23:
					System.out.println("Enter the VM name to remove Alarm");
					vmName = br.readLine();
					server.removeAlarm(vmName);		
					break;

					//check VM CPU Usage
				case 24:
					System.out.println("Enter the VM name to check the CPU Usage");
					vmName = br.readLine();
					System.out.println(server.getVMCPUUsage(vmName));		
					break;

					//Check the Host CPU Usage
				case 25:
					System.out.println("Enter the Host Ip to check the CPU Usage ");
					hostIp = br.readLine();
					System.out.println(server.getHostCPUUsage(hostIp));	
					break;


				}

			}
			catch(Exception e){
				System.out.println(e);
				e.getStackTrace();
				e.getMessage();
			}
		}
	}
}
