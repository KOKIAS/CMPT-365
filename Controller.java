package application;

import javax.swing.JFileChooser;
import org.opencv.core.CvType;
import org.opencv.core.Mat;// matrix
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import utilities.Utilities;

import java.io.*;
import java.util.ArrayList;

public class Controller {
	
	@FXML
	private ImageView imageView; // the image display window in the GUI	
	
	private Mat image;
	
	private int histBin;
	private int totalFrame ; //there must exist atleast 1 frame -> which is an image.
	private static int frameNumber; // The counter uses to count frames
	private VideoCapture capture; 
	
	private Mat colSTI; // MAT for copying pixel STI (column)
	private Mat rowSTI; // MAT for copying pixel STI (row)
	private Mat colHistSTI;// MAT for Histogram pixel STI (column)
	private Mat rowHistSTI; // MAT for Histogram pixel STI (row)
	private ArrayList<Mat> frames;//storing all the frames(after chromaticity) for later use (HISTOGRAM)
	private boolean newCP;
	
	private String getImageFilename() {
		// This method should return the filename of the image to be played
		// You should insert your code here to allow user to select the file
		FileChooser fc = new FileChooser();
		
		File selectedFile = fc.showOpenDialog(null);
		if(selectedFile != null) {
			String fileName = selectedFile.getAbsolutePath();//gets the absolute path of the file selected.
			return fileName;
		}
		else {
			System.out.println("File Selection Fail");//file cannot be opened.
			return null;
		}
	}
	@FXML
	protected void openImage(ActionEvent event) {
		// This method opens an image and display it using the GUI
		// You should modify the logic so that it opens and displays a video
		final String imageFilename = getImageFilename();
		String extension = imageFilename.substring(imageFilename.lastIndexOf("."),imageFilename.length()); 
		//checks file extension to see if it is a video. ( included the top 5 extension for videos
		if (extension.equals(".mp4") || extension.equals(".wmv") || extension.equals(".mov") || extension.equals(".mpeg") || extension.equals(".avi")) {
			capture = new VideoCapture(imageFilename);//open video file
			if(capture.isOpened()) {//open successfully
				newCP=true;
				createFrameGrabber();
				histSTI(null);
			}
			else
				System.out.println("capture is not opened");
		}
		else
			System.out.println("This filetype is not supported, This is a type "+extension);
	}
	//using center column to configure the STI
	//using center column to configure the STI
	//This function display the copy pixel column image
	@FXML
	protected void centerCOL_STI(ActionEvent event){
		imageView.setImage(Utilities.mat2Image(colSTI));// Suppose to display a center column STI
	}
	
	//using center row to configure the STI
	//This function display the copy pixel row image
	@FXML
	protected void centerROW_STI(ActionEvent event){
		imageView.setImage(Utilities.mat2Image(rowSTI)); // Suppose to display a center row STI
	}
	//using Histogram row to configure the STI
	//using histogram difference to configure the STI (for rows)
	//This function display the histRow image
	@FXML
	protected void histROW_STI(ActionEvent event) {
		imageView.setImage(Utilities.mat2Image(rowHistSTI)); // Suppose to display a Histogram row STI
	}
	
	//using histogram difference to configure the STI (for rows)
	//using center column to configure the STI
	//This function display the histCOL image
	@FXML
	protected void histCOL_STI(ActionEvent event) {
		imageView.setImage(Utilities.mat2Image(colHistSTI)); // Suppose to display a Histogram row STI
	}
	
	//The function for forming / filling in the matrix for the STI
	//The Function that copy pixels and make STI
	//This function calculates the copy Pixel STI
	protected void copyPixel(ActionEvent event) {
		if (image != null) {
			//filling matrix for center column STI
			Mat col = image.col(Math.round(image.cols()/2)); // this is the center column of this frame

			if(newCP == true) { // if this is the 1st frame coming in , we initialize the MAT
				colSTI = new Mat (image.rows(),totalFrame,CvType.CV_8UC3);
			}
			//puting this center column in to the STI@"frameNumber" column
			col.col(0).copyTo(colSTI.col(frameNumber));
			
			//filling matrix for center row STI
			Mat row = image.row(Math.round(image.rows()/2));// this is the center row of this frame
			if(newCP == true) {// if this is the 1st frame coming in , we initialize the MAT
				rowSTI = new Mat (totalFrame,image.cols(),CvType.CV_8UC3);
			}
			//putting this center row in to the STI@"frameNumber" column
			row.row(0).copyTo(rowSTI.row(frameNumber));
			
			if(newCP == true)
				newCP = false;
			
		}
		else
			System.out.println("No image is selected");
		}
	
	//The function that do the math for chromaticity - 
	protected void chromaticity(ActionEvent event) {//create the chromaticity image for fame and call the histogram function		
		if(frameNumber == 0)
			frames = new ArrayList<>();
		if (image != null) {
			Mat chrom = new Mat (image.rows(),image.cols(),CvType.CV_64FC2); // Mat object that stores the chromaticity values for this frame 
			//convert the mat from CV_8UC3/CV_8UC2 to CV_64FC3/CV_64FC2 
			//CV_xxTCn : xx is the number of bit, 
			//T is the type (F-float , S-signedInterger, U-unsignedInterger)
			//C means channel and n is the number of channels.
			image.convertTo(image, CvType.CV_64FC3);
			
			//convert all rgb (from colSTI (3channel) in to an array)
			int C3_Size = (int) (image.total()*image.channels());
			double []  C3_Temp = new double [C3_Size];
			image.get(0, 0,C3_Temp);
			
			//set up array for storing values for the chromaticity MAT
			int C2_Size = (int)(chrom.total()*chrom.channels());
			double [] C2_Temp = new double [C2_Size];
			
			
			//Do the chromaticity calculation
			int counter = 1; // counter for going through C3_Temp. every 3 i is a pixel.
			int C2_counter = 0; //counter for putting values into C2_Temp
			for(int i = 0 ; i< C3_Size ; i++) {
				if (counter % 3 == 0) { // every 3 counter is 1 pixel which contains values RGB
					//Getting valuese RGB for "this" pixel
					double B =  C3_Temp [i];
					double G =  C3_Temp [i-1];
					double R =  C3_Temp [i-2];
					//chromaticity function
					double r, g; 
					if(R==0 && G==0 && B==0) { // special case when color is black
						r = 0.00;
						g = 0.00;
					}
					else { // colors other then black.
						r = R / (R+G+B);
						g = G/(R+G+B);
					}
					if(C2_counter < C2_Size) {// a checker for counter went out of bound.
						//Putting rg values into array.
						C2_Temp[C2_counter] = r;
						C2_counter ++;
						C2_Temp[C2_counter] = g;
						C2_counter++;
					}
				}
				counter++;
			}
			//putting the array of RG values into colSTI_chromaticity MAT.
			chrom.put(0, 0, C2_Temp);
			//convert back to 3 channel MAT
			image.convertTo(image, CvType.CV_8UC3);
			frames.add(chrom);
		}
		else
			System.out.println("No image is selected");
	}
	
	//The function that do the difference intersection
	//This function calculates the intersection Differenct
	protected void diffIntersection(ArrayList<double[][]> Histogram , ArrayList<Double> I) {// array of scalar I
		for(int i = 0 ; i < totalFrame - 1 ; i++) {//going through all the frames
			double[][] previous = Histogram.get(i);
			double[][] current = Histogram.get(i+1);
			//column Histogram STI
			double sumI = 0.0;
			for(int row = 0 ; row < histBin ; row++)
				for(int col = 0 ; col < histBin ; col ++)
					 sumI+= Math.min(previous[row][col], current[row][col]);
			I.add(sumI);
		}
	}
	//The function that creates histograms
	protected void createHistogram(ArrayList<double[][]> Histogram , Mat chrom , int n) { // histogram is colHistogram or rowHistogram , sti should be chromaticity Mat
		// n = size of data ~ number of image rows
		histBin = (int)Math.floor(1+((double)Math.log(n)/(double)Math.log(2))); // Sturge's Rule - > N = 1+log2(n)
		int r_axis , g_axis; //col and row for histogram. where r value is row and green value is column
		double boundary = 1.00/histBin; // each histogram has n number of bin , we divide the max range by bin to find the boundary value for each bin
		
		//Creating Histogram for each frame
		int Size = (int) (chrom.total()*chrom.channels());
		double []  Temp = new double [Size]; 
		chrom.get(0, 0,Temp);//get the rg values for column(i) ~ array of values
		double [][] hist  = new double [histBin][histBin];//the "object"(2d array) to be put into array list
		//going through all the rg values in "this" column , and filling the 2-d array
		for(int rgValue = 0 ; rgValue < Size-1 ; rgValue = rgValue+2) {
			double rValue = Temp[rgValue] , gValue = Temp[rgValue+1];
			r_axis = (int)Math.floor(rValue/boundary);
			g_axis = (int)Math.floor(gValue/boundary);
			if(r_axis == histBin)//boundary case , color values toward that the floor is overflow is put into last bin
				r_axis = histBin-1;
			if(g_axis == histBin)
				g_axis = histBin-1;
			hist[r_axis][g_axis]++;
		}
		double sum = 0;
		for(int i=0 ; i<histBin ; i++)
			for(int j=0 ; j<histBin ; j++)
				sum+=hist[i][j];
		
		for(int i=0 ; i<histBin ; i++)
			for(int j=0 ; j<histBin ; j++)
				hist[i][j]= hist[i][j]/sum;
		
			Histogram.add(hist);// Adding hist object to array list histogram
	}
	//The function that decompose the video in to different frames
	protected void createFrameGrabber(){
		frameNumber = 0;
		if(capture != null && capture.isOpened()) {//the video must be opened
			totalFrame = (int) Math.round(capture.get(Videoio.CAP_PROP_FRAME_COUNT));
			//create a runnable to fetch new frames periodically
			Runnable frameGrabber = new Runnable() {
				@Override
				public void run() {
					Mat frame = new Mat();
					if (capture.read(frame)) {//decode successfully
							if(frameNumber == 0) {
								image = frame;
							}
							copyPixel(null);
							chromaticity(null);
							image = frame;
							frameNumber++;
							
						}
					else {capture.release();} //reach the end of the video
				}
			};
			//loop through all the frames avliable for this video.
			for(int i = 0 ; i < totalFrame ; i++)
				frameGrabber.run();
		}
	}
	//This function calculates the histogram STI
	protected void histSTI(ActionEvent event){
		ArrayList<ArrayList<Double>> col_I = new ArrayList<ArrayList<Double>>();
		ArrayList<ArrayList<Double>> row_I = new ArrayList<ArrayList<Double>>();

		// column STI
		colHistSTI = new Mat (32,totalFrame,CvType.CV_8UC3);
		for(int column = 0 ; column < 32; column++) {
			ArrayList<Mat> frameColumns = new ArrayList<>();	//stores columns for each frame
			
			//convert frame to frame number of columns
			for(int i = 0 ; i <totalFrame ; i++) {  //getting the same column for all frames
				Mat resizedImage = new Mat();
				Imgproc.resize(frames.get(i), resizedImage, new Size(32,32));
				frameColumns.add(resizedImage.col(column));
			}
			//creating histogram for each frame column
			ArrayList<double[][]> histogram = new ArrayList<>();
			for(int i = 0 ; i< totalFrame ; i++) 
				createHistogram(histogram , frameColumns.get(i), frameColumns.get(i).rows());

			//calculating I for each column
			ArrayList<Double> I = new ArrayList<>();
			diffIntersection(histogram , I);

		/*	//thresholded version
			for(int i = 0 ; i<I.size(); i++) {
				double value = I.get(i);
				if(value<0.7)
					I.set(i, 0.0);
				else
					I.set(i, 1.0);
			}*/
				
			col_I.add(I);//col_I will hold the I for the entire image after the loop.
		}
		//Filling in the image
		for(int i = 0; i < col_I.size(); i++)
			for(int j = 0 ; j < col_I.get(0).size() ; j++) {
				double value = col_I.get(i).get(j) *255;
				double [] data = {value,value,value};
				colHistSTI.put(i, j, data);
		}
	
		//row STI
		rowHistSTI = new Mat (32,totalFrame,CvType.CV_8UC3);
		for(int row = 0 ; row < 32; row++) {
			ArrayList<Mat> frameRow = new ArrayList<>();	//stores columns for each frame
			
			//convert frame to frame number of columns
			for(int i = 0 ; i <totalFrame ; i++) {  //getting the same column for all frames
				Mat resizedImage = new Mat();
				Imgproc.resize(frames.get(i), resizedImage, new Size(32,32));
				frameRow.add(resizedImage.row(row));
			}
			
			//creating histogram for each frame column
			ArrayList<double[][]> histogram = new ArrayList<>();
			for(int i = 0 ; i< totalFrame ; i++)
				createHistogram(histogram , frameRow.get(i),frameRow.get(i).cols());
			

			//calculating I for each column
			ArrayList<Double> I = new ArrayList<>();
			diffIntersection(histogram , I);
			row_I.add(I);//col_I will hold the I for the entire image after the loop.
			
		}

		for(int i = 0; i < row_I.size(); i++)
			for(int j = 0 ; j < row_I.get(0).size() ; j++) {
				double value = row_I.get(i).get(j) *255;
				double [] data = {value,value,value};
				rowHistSTI.put(i, j, data);
			}
	}
}
	