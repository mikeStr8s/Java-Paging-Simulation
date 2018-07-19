import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Stream;

public class PagingSimulation {
    private JFrame simWindow = new JFrame("Paging Simulation"); //creates the simulation window
    private JLabel[] FTDArray = new JLabel[16]; //Array of Frame table data entries for the simulation window
    private JLabel[] newPageTable = new JLabel[64]; //Array of possible page table indices
    private JLabel PIDLabel = new JLabel("Process: ___");   //label for displaying process ID
    private JLabel pageReferenced = new JLabel("Process: ___\nPage: ___");  //label for displaying referenced process ID and page #
    private JLabel procPageStatsLabel = new JLabel(""); //process page statistics label
    private JLabel procPageRefStatsLabel = new JLabel("");  //page reference statistic label
    private JLabel procFaultStatsLabel = new JLabel("");    //total page fault statistic
    private JLabel pageFaultsPerProc = new JLabel("");  //page faults per process label
    private JLabel victimLabel = new JLabel("");    //victim page label
    private ArrayList<String> inputData = new ArrayList<>();    //Array to hold all of the page references
    private ArrayList<String> FrameTable = new ArrayList<>();   //Array of the frame table values
    private HashMap<String, Integer> frameTableUsage = new HashMap<>();   //Hashmap of the frame table values as their ranking in the LRU
    private HashMap<String, ArrayList<Integer>> PCBhashmap = new HashMap<>();    //Structure {PID: {frame#, frame#, frame#}
    private HashMap<String, Integer> procMemRefs = new HashMap<>(); //data structure for the  total process memory references
    private HashMap<String, Integer> procPageFaults = new HashMap<>();  //data structure for the total process page faults
    private JPanel pagePanel = new JPanel(new GridLayout());  //container for the page table
    private JPanel framePanel = new JPanel(new BorderLayout()); //container for the frame table
    private JPanel firstSixteen = new JPanel( new GridLayout(0,2)); //create panel to hold the first 16 pages
    private JPanel secondSixteen = new JPanel(new GridLayout(0,2)); //create panel to hold the second 16 pages
    private JPanel thirdSixteen = new JPanel(new GridLayout(0,2));  //create panel to hold the third 16 pages
    private JPanel fourthSixteen = new JPanel(new GridLayout(0,2)); //create panel to hold the fourth 16 pages
    private int count = 0;  //the count to keep track of where the simulation stopped
    private int totalFaults = 0;    //total faults counter
    private boolean isFault = false;    //used to detect a fault

    public static void main(String[] args) {
        PagingSimulation PS = new PagingSimulation();
        PS.populateData();
        PS.createGUI();
    }

    private void runSimulationStep(){
        if(count < inputData.size()){   //while the line count is less than the total lines in the reference file
            computeFrameTable(inputData.get(count));    //compute the frame table at the given line
            count++;    //increment line counter
        }
        if(count == inputData.size()){  //if the entire page reference file has been read
            reportStatistics(); //post simulation statistics to the user
        }
    }

    private void runSimulation(){
        for(int i = count; i < inputData.size(); i++){  //while the line count is less than the total lines in the reference file
            computeFrameTable(inputData.get(i));    //compute the frame table at the given line
            count++;    //increment the line counter
        }
        reportStatistics(); //post simulation statistics to the user
    }

    private void runSimulationFault(){
        isFault = false;    //reset the fault notifier to false
        for(int i = count; i < inputData.size(); i++){  //while the line count is less than the total lines in the reference file
            if(!isFault) {  //if a fault is not detected
                computeFrameTable(inputData.get(i));    //compute the frame table at the given line
                count++;    //increment the line counter
            }
        }
        if(count == inputData.size()){  //if the entire page reference file has been read
            reportStatistics(); //post the simulation statistics to the user
        }
    }

    private void populateData(){
        try (Stream<String> stream = Files.lines(Paths.get("input3b.data"))) {  //read the input line by line into a Stream
            stream.forEach(data -> {
                int colon = data.indexOf(":");  //to find the end of the PID substring
                int tab = data.indexOf("\t") + 1;   //to find the beginning of the page data
                String PID = data.substring(0,colon);   //extract the PID substring
                String pageNum = data.substring(tab);   //extract the Page Number
                int page = Integer.parseInt(pageNum, 2);

                inputData.add(PID+"\t"+"Page "+page);
            });    //For each line in the stream add them to the arraylist
        } catch (IOException e) {   //if there is an error print it
            System.out.println(e);
        }

        for (String pageRef: inputData) {
            int tab = pageRef.indexOf("\t");  //to find the end of the PID substring
            int ref = pageRef.indexOf(" ") +1;   //to find the beginning of the page data
            String PID = pageRef.substring(0, tab); //store PID
            String page = pageRef.substring(ref);   //store page
            int pageNum = Integer.parseInt(page);   //parse integer from page

            //populate pcb with null values
            if(!PCBhashmap.containsKey(PID)){   //if the pcb data structure does not contain the process
                PCBhashmap.put(PID, new ArrayList<>()); //create key of PID and empty page table arraylist
                PCBhashmap.get(PID).add(pageNum, -1);   //insert -1 into unused page table pages
            }
            else{
                if(PCBhashmap.get(PID).size() <= pageNum){  //if there is a new pagepage
                    PCBhashmap.get(PID).add(pageNum, -1);   //add new page to page table with -1 value
                }
            }
        }
    }

    private void computeFrameTable(String pageRef){
        if(frameTableUsage.size() <= 15){   //if the frame table is not full
            if(!frameTableUsage.containsKey(pageRef)){  //if the frame table does not contain the page reference
                frameTableUsage.forEach((k,v) -> frameTableUsage.put(k, frameTableUsage.get(k)+1)); //for each key value pair add 1 to the value
                frameTableUsage.put(pageRef,0); //add new page reference with priority 0
                FrameTable.add(pageRef);    //add page reference to frame table
                FTDArray[FrameTable.indexOf(pageRef)].setText(pageRef); //display frame table entries
                isFault = true; //trigger fault
                totalFaults++;  //increment total fault

                int pidLocation = pageRef.indexOf("\t");    //get location of the PID of incoming process
                int pageLocation = pageRef.indexOf(" ");    //get location of the page # of the incoming process
                String PID = pageRef.substring(0, pidLocation); //get the PID of the incoming process
                String page = pageRef.substring(pageLocation+1);    //get the String of the page #
                int pageNum = Integer.parseInt(page);   //get the integer of the page#

                procPageFaults.putIfAbsent(PID,0);  //if the process does not exist in the data structure put it there and set value to 0
                procPageFaults.put(PID, procPageFaults.get(PID)+1); //add one to the total page faults in data structure for the process
            }
            else{
                int priorityVal = frameTableUsage.get(pageRef); //value for replacement delimiter
                frameTableUsage.forEach((k,v) -> {  //for each entry in the frame table
                    if(frameTableUsage.get(k) < priorityVal){   //if the entry has priority below duplicate value
                        frameTableUsage.put(k, frameTableUsage.get(k)+1);   //add one to the priority value
                    }});
                frameTableUsage.put(pageRef,0); //change priority value to 0
            }
            int pidLocation = pageRef.indexOf("\t");    //get location of the PID of incoming process
            int pageLocation = pageRef.indexOf(" ");    //get location of the page # of the incoming process
            String PID = pageRef.substring(0, pidLocation); //get the PID of the incoming process
            String page = pageRef.substring(pageLocation+1);    //get the String of the page #
            int pageNum = Integer.parseInt(page);   //get the integer of the page#

            PCBhashmap.get(PID).set(pageNum, FrameTable.indexOf(pageRef));  //add the page reference to the page table
            procMemRefs.putIfAbsent(PID,0); //if the process does not exist in the data structure already, add it and place default 0
            procMemRefs.put(PID, procMemRefs.get(PID)+1);   //add one to process reference total

            updatePageTable(pageRef);   //updates the page table for the given process

            pageReferenced.setText("Process: "+PID+"\nPage: "+pageNum); //display current working process and page number
        }
        else{   //frame table is full
            if(!frameTableUsage.containsKey(pageRef)){  //if the frame table does not contain the page reference
                frameTableUsage.forEach((k,v) -> frameTableUsage.put(k, frameTableUsage.get(k)+1)); //for each key value pair add 1 to the value
                final String[] entry = {""};
                frameTableUsage.forEach((k,v) -> {  //for each entry in frame table
                    if(v == 16){    //if the value of the entry is 16
                        int index = FrameTable.indexOf(k);  //index value of frame table entry k
                        entry[0] = k;   //store entry to be replaced
                        FrameTable.set(index, pageRef); //replace Frame table entry
                    }});
                frameTableUsage.remove(entry[0]);   //remove frame table usage entry
                frameTableUsage.put(pageRef,0); //add new page reference with priority 0
                FTDArray[FrameTable.indexOf(pageRef)].setText(pageRef); //display frame table entries

                int pidLocation = entry[0].indexOf("\t");    //get location of the PID of the process being removed
                int pageLocation = entry[0].indexOf(" ");    //get location of the page # of the process being removed
                String PID = entry[0].substring(0, pidLocation); //get the PID of the process being removed
                String page = entry[0].substring(pageLocation+1);    //get the String of the page # of the process being removed
                int pageNum = Integer.parseInt(page);   //get the integer of the page#  being removed
                victimLabel.setText("Victim: "+PID+" Page "+pageNum);   //display the victim process and page #

                PCBhashmap.get(PID).set(pageNum, -1);  //remove the page reference in the page table

                pidLocation = pageRef.indexOf("\t");    //get location of the PID of incoming process
                pageLocation = pageRef.indexOf(" ");    //get location of the page # of the incoming process
                PID = pageRef.substring(0, pidLocation); //get the PID of the incoming process
                page = pageRef.substring(pageLocation+1);    //get the String of the page #
                pageNum = Integer.parseInt(page);   //get the integer of the page#

                PCBhashmap.get(PID).set(pageNum, FrameTable.indexOf(pageRef));  //add the page reference to the page table
                procMemRefs.putIfAbsent(PID,0); //if the process does not exist in the data structure already, add it and place default 0
                procMemRefs.put(PID, procMemRefs.get(PID)+1);   //add one to process reference total
                procPageFaults.putIfAbsent(PID,0);  //if the process does not exist in the data structure put it there and set value to 0
                procPageFaults.put(PID, procPageFaults.get(PID)+1); //add one to the total page faults in data structure for the process

                updatePageTable(pageRef);   //updates the page table for the given process
                pageReferenced.setText("Process: "+PID+"\nPage: "+pageNum); //display current working process and page number

                isFault = true; //indicate that a fault has occurred
                totalFaults++;  //increase total fault value
            }
            else{
                int priorityVal = frameTableUsage.get(pageRef); //value for replacement delimiter
                frameTableUsage.forEach((k,v) -> {  //for each entry in the frame table
                    if(frameTableUsage.get(k) < priorityVal){   //if the entry has priority below duplicate value
                        frameTableUsage.put(k, frameTableUsage.get(k)+1);   //add one to the priority value
                    }});
                frameTableUsage.put(pageRef,0); //change priority value to 0

                int pidLocation = pageRef.indexOf("\t");    //get location of the PID of incoming process
                int pageLocation = pageRef.indexOf(" ");    //get location of the page # of the incoming process
                String PID = pageRef.substring(0, pidLocation); //get the PID of the incoming process
                String page = pageRef.substring(pageLocation+1);    //get the String of the page #
                int pageNum = Integer.parseInt(page);   //get the integer of the page#
                pageReferenced.setText("Process: "+PID+"\nPage: "+pageNum);
                procMemRefs.putIfAbsent(PID,0); //if the process does not exist in the data structure already, add it and place default 0
                procMemRefs.put(PID, procMemRefs.get(PID)+1);   //add one to process reference total

                victimLabel.setText("");    //clear the victim label as no victim was present
            }
        }
    }

    private void updatePageTable(String pageRef){
        int pidLocation = pageRef.indexOf("\t");    //get location of the PID of incoming process
        int pageLocation = pageRef.indexOf(" ");    //get location of the page # of the incoming process
        String PID = pageRef.substring(0, pidLocation); //get the PID of the incoming process
        String page = pageRef.substring(pageLocation+1);    //get the String of the page #
        int pageNum = Integer.parseInt(page);   //get the integer of the page#

        PIDLabel.setText("Process: "+PID);  //set the process id in the page table title

        for(int k = 0; k < 64; k++){    //create all page table entry labels
            newPageTable[k].setText("");    //empty all page table entry labels
        }

        int i = 0;  //incremental value
        for (int frameStored: PCBhashmap.get(PID)) {    //for every int in the PCB data structure for a given process PID
            newPageTable[i].setText(""+PCBhashmap.get(PID).get(i)); //set the text to the respective page table entry to the current frame that the page is held
            i++;    //increment the incremental value
        }
    }

    private void reportStatistics(){
        final String[] totalProcRefs = {"Proc Pages: ","Proc Refs: ","Faults per Proc: ", ""};  //Array of strings holding data for statistic information
        PCBhashmap.forEach((k,v) -> {   //for each PID in the PCB
            totalProcRefs[0] += k+":"+v.size()+", ";    //concat string structure like: Proc Pages: PID:#pages,...
        });
        procMemRefs.forEach((k,v) -> {  //for each PID in the process memory reference data structure
            totalProcRefs[1] += k+":"+v+", ";   //concat string structure like: Proc Refs: PID:#references,...
        });
        procPageFaults.forEach((k,v) -> {   //for each PID in the process page fault data structure
            totalProcRefs[2] += k+":"+v+", ";   //concat string structure like: Faults per Proc: PID:#faults,...
        });
        totalProcRefs[0] = totalProcRefs[0].substring(0, totalProcRefs[0].length()-2);  //remove trailing ", "
        totalProcRefs[1] = totalProcRefs[1].substring(0, totalProcRefs[1].length()-2);  //remove trailing ", "
        totalProcRefs[2] = totalProcRefs[2].substring(0, totalProcRefs[2].length()-2);  //remove trailing ", "
        totalProcRefs[3] = "Total Faults: "+totalFaults;    //concat total page fault output

        procPageStatsLabel.setText(totalProcRefs[0]);   //change text of label
        procPageRefStatsLabel.setText(totalProcRefs[1]);    //change text of label
        pageFaultsPerProc.setText(totalProcRefs[2]);    //change text of label
        procFaultStatsLabel.setText(totalProcRefs[3]);  //change text of label

        simWindow.pack();   //resize window to fit
    }

    private void createGUI(){
        /**
         * This method handles the creation of all GUI elements
         * */
        simWindow.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);   //make the exit button into the exit() command
        simWindow.setLayout(new GridBagLayout());   //set layout for simulation window
        GridBagConstraints jfConst = new GridBagConstraints();  //create constraints for layout
        Font titles = new Font("Courier", Font.BOLD, 18);   //create a font for table titles
        Border raisedBevel = BorderFactory.createRaisedBevelBorder();   //create border style for cell data

        /**
         * Create, modify, and implement the UI elements for the Physical Memory/Frame Table section of the Simulation Window.
         * This part of the UI holds all of the labels for the frames in the Frame Table, and all of the Data that is
         * inside of those frames.
         * */
        JLabel physMem = new JLabel("Physical Memory");    //create physical memory title
        physMem.setFont(titles);    //apply font to title
        jfConst.gridx = 0;  //-----\
        jfConst.gridy = 0;  //------Set location and width of UI element
        jfConst.gridwidth = 2;//---/
        simWindow.getContentPane().add(physMem, jfConst);   //title for the physical memory section

        jfConst.gridy = 1;  //move down 1 grid block
        JLabel ftablelabel = new JLabel("Frame Table");
        simWindow.getContentPane().add(ftablelabel, jfConst);

        jfConst.gridy = 2;  //move down 1
        GridLayout gridLayout = new GridLayout(0,2);    //create a grid layout
        framePanel.setLayout(gridLayout);   //set layout of panel to grid layout
        simWindow.getContentPane().add(framePanel,jfConst); //add frame panel to simulation window
        JLabel[] FTLArray = new JLabel[16]; //creating Frame Table Frames 0-15
        for(int i = 0; i < FTLArray.length; i++){
            FTLArray[i] = new JLabel("Frame "+i);   //create label reading Frame 0-15
            FTDArray[i] = new JLabel("   \t      ");    //add placeholder text to the label
            FTDArray[i].setBorder(raisedBevel); //set label border to raised
            framePanel.add(FTLArray[i]);    //add jlabel to frame panel
            framePanel.add(FTDArray[i]);    //add jlabel to frame panel
        }

        /**
         * Create, modify, and implement the UI elements for the current process and page reference
         * */
        JLabel pageReferenceTitle = new JLabel("Page Referenced");  //title for the page referenced section
        jfConst.gridx = 2;
        jfConst.gridy = 0;
        jfConst.gridwidth = 2;
        jfConst.insets = new Insets(0,25,0,25); //add padding to either side of the item
        pageReferenceTitle.setFont(titles);
        simWindow.getContentPane().add(pageReferenceTitle, jfConst);    //add the title to the simulation window

        jfConst.gridy = 1;
        simWindow.getContentPane().add(pageReferenced, jfConst);    //add the page referenced label to simulation window
        jfConst.gridy++;
        simWindow.getContentPane().add(victimLabel, jfConst);   //add the victim label to simulation window


        /**
         * Create, modify, and implement the UI elements for the Page table section of the Simulation Window.
         * This part of the UI holds all of the labels for the pages in the page table, and all of the data
         * that is inside of those pages.
         * */
        jfConst.gridx = 4;  //next to frame table title out the layout x component
        jfConst.gridy = 0;  //zero out the layout y component
        jfConst.gridwidth = 2;  //set width to two columns
        jfConst.insets = new Insets(0,25,0,0); //add padding to either side
        JLabel pageTableTitle = new JLabel("Page Table");  //create the "Page Table" title
        pageTableTitle.setFont(titles); //set font to title font
        simWindow.getContentPane().add(pageTableTitle, jfConst);    //add to simulation window

        jfConst.gridy = 1;
        simWindow.getContentPane().add(PIDLabel, jfConst);  //add the PID label to the simulation window

        jfConst.gridy = 2;
        jfConst.gridwidth = 1;
        GridLayout pageGridLayout = new GridLayout(0,4);    //create grid layout with any number of rows and only 4 columns
        pagePanel.setLayout(pageGridLayout);    //set panel layout
        simWindow.getContentPane().add(pagePanel,jfConst);  //add panel to simulation window

        for(int i = 0; i < newPageTable.length; i++){   //for every entry in the Page table structure
            JLabel temp = new JLabel("Page "+i);    //create label naming each corresponding index of the page table
            newPageTable[i] = new JLabel("   \t      ");    //set default value in each page table entry
            newPageTable[i].setBorder(raisedBevel); //add a raised beveled edge to the labels
            if(i < 16) {    //if the page table entry is 0-15
                firstSixteen.add(temp); //add entry name
                firstSixteen.add(newPageTable[i]);  //add entry data
            }
            else if(i < 32){    //if the page table entry is 16-31
                secondSixteen.add(temp);    //add entry name
                secondSixteen.add(newPageTable[i]); //add entry data
            }
            else if(i < 48){    //if the page table entry is 32-47
                thirdSixteen.add(temp); //add entry name
                thirdSixteen.add(newPageTable[i]);  //add entry data
            }
            else{   //if table entry is < 47
                fourthSixteen.add(temp);    //add entry name
                fourthSixteen.add(newPageTable[i]); //add entry data
            }
        }
        pagePanel.add(firstSixteen);    //add the first 16 page table entries
        pagePanel.add(secondSixteen);   //add the second 16 page table entries
        pagePanel.add(thirdSixteen);    //add the third 16 page table entries
        pagePanel.add(fourthSixteen);   //add the fourth 16 page table entries

        jfConst.gridy = 3;
        simWindow.getContentPane().add(procPageStatsLabel, jfConst);    //add the page statistics label
        jfConst.gridy++;
        simWindow.getContentPane().add(procPageRefStatsLabel, jfConst); //add the page reference statistics label
        jfConst.gridy++;
        simWindow.getContentPane().add(pageFaultsPerProc, jfConst); //add the page faults per process label
        jfConst.gridy++;
        simWindow.getContentPane().add(procFaultStatsLabel, jfConst);   //add the total page fault statistics label


        /**This creates, modifies, and implements all of the buttons for the UI*/
        JButton runSim = new JButton("Run To End"); //create the run full simulation button
        runSim.addActionListener(new ActionListener() { //add a click listener
            @Override
            public void actionPerformed(ActionEvent e) {    //when clicked
                runSimulation();    //run full simulation
            }
        });
        JButton runSimStep = new JButton("Step");   //create the run step simulation button
        runSimStep.addActionListener(new ActionListener() { //add click listener
            @Override
            public void actionPerformed(ActionEvent e) {    //when clicked
                runSimulationStep();    //run step simulation
            }
        });
        JButton runSimFault = new JButton("Run To Fault");  //create the run to fault simulation button
        runSimFault.addActionListener(new ActionListener() {    //add click listener
            @Override
            public void actionPerformed(ActionEvent e) {    //when clicked
                runSimulationFault();   //run the run to fault simulation
            }
        });
        jfConst.gridx = 0;
        jfConst.gridy = 3;
        jfConst.gridwidth = 2;
        simWindow.getContentPane().add(runSim, jfConst);    //add button to run full cycle of simulation
        jfConst.gridy = 4;
        simWindow.getContentPane().add(runSimStep,jfConst); //add button to run step simulation
        jfConst.gridy = 5;
        simWindow.getContentPane().add(runSimFault, jfConst);   //add button to run to fault

        //display window
        simWindow.pack();   //size window according to content
        simWindow.setVisible(true); //make window visible
    }
}