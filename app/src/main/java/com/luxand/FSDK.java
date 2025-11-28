/*
 * FaceSDK Library Interface
 * Copyright (C) 2023 Luxand, Inc.
 */

package com.luxand;

import android.app.Application;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class FSDK
{
	static {
		//System.loadLibrary("stlport_shared");
		System.loadLibrary("fsdk");
	}


	// Error codes

	public static final int FSDKE_OK = 0;
	public static final int FSDKE_FAILED = -1;
	public static final int FSDKE_NOT_ACTIVATED = -2;
	public static final int FSDKE_OUT_OF_MEMORY	= -3;
	public static final int FSDKE_INVALID_ARGUMENT = -4;
	public static final int FSDKE_IO_ERROR = -5;
	public static final int FSDKE_IMAGE_TOO_SMALL = -6;
	public static final int FSDKE_FACE_NOT_FOUND = -7;
	public static final int FSDKE_INSUFFICIENT_BUFFER_SIZE = -8;
	public static final int FSDKE_UNSUPPORTED_IMAGE_EXTENSION =	-9;
	public static final int FSDKE_CANNOT_OPEN_FILE = -10;
	public static final int FSDKE_CANNOT_CREATE_FILE = -11;
	public static final int FSDKE_BAD_FILE_FORMAT = -12;
	public static final int FSDKE_FILE_NOT_FOUND = -13;
	public static final int FSDKE_CONNECTION_CLOSED = -14;
	public static final int FSDKE_CONNECTION_FAILED = -15;
	public static final int FSDKE_IP_INIT_FAILED = -16;
	public static final int FSDKE_NEED_SERVER_ACTIVATION = -17;
	public static final int FSDKE_ID_NOT_FOUND = -18;
	public static final int FSDKE_ATTRIBUTE_NOT_DETECTED = -19;
	public static final int FSDKE_INSUFFICIENT_TRACKER_MEMORY_LIMIT = -20;
	public static final int FSDKE_UNKNOWN_ATTRIBUTE = -21;
	public static final int FSDKE_UNSUPPORTED_FILE_VERSION = -22;
	public static final int FSDKE_SYNTAX_ERROR = -23;
	public static final int FSDKE_PARAMETER_NOT_FOUND = -24;
	public static final int FSDKE_INVALID_TEMPLATE = -25;
	public static final int FSDKE_UNSUPPORTED_TEMPLATE_VERSION = -26;
	public static final int FSDKE_CAMERA_INDEX_DOES_NOT_EXIST = -27;
	public static final int FSDKE_PLATFORM_NOT_LICENSED = -28;
	public static final int FSDKE_TENSORFLOW_NOT_INITIALIZED = -29;
	public static final int FSDKE_FACEID_NOT_FOUND = -32;
	public static final int FSDKE_FACEIMAGE_NOT_FOUND = -33;

	// Facial feature count

	public static final int FSDK_FACIAL_FEATURE_COUNT = 70;


	// Types

	public static class FSDK_VIDEOCOMPRESSIONTYPE {
		public static final int FSDK_MJPEG = 0;
		public int type;
	}

	public static class FSDK_IMAGEMODE {
		public static final int FSDK_IMAGE_GRAYSCALE_8BIT = 0;
		public static final int FSDK_IMAGE_COLOR_24BIT = 1;
		public static final int FSDK_IMAGE_COLOR_32BIT = 2;
		public int mode;
	}

	public static class HImage {  //to pass himage "by reference"
		protected int himage;
	}

	public static class HCamera {  //to pass hcamera "by reference"
		protected int hcamera;
	}

	public static class HTracker {
		protected int htracker;
	}

	public static class TFacePosition {
		public int xc, yc, w;
		public int padding;
		public double angle;
	}

	public static class TFaces {
		public TFacePosition faces[];
		public int maxFaces;
		public TFaces(){
			maxFaces = 100;
			faces = null;
		}
		public TFaces(int MaxFaces){
			maxFaces = MaxFaces;
			faces = null;
		}
	}

	public static class TPoint {
		public int x, y;
	}

	public static class BBox {
		public TPoint p0 = new TPoint();
		public TPoint p1 = new TPoint();
	}

	public static class TFace {
		public BBox bbox = new BBox();
		public TPoint features[] = new TPoint[5];

		{
			for (var i = 0; i < features.length; ++i)
				features[i] = new TPoint();
		}
	}

	public static class TFaces2 {
		public TFace faces[] = null;
		public int maxFaces = 100;

		public TFaces2() {}

		public TFaces2(int maxFaces) {
			this.maxFaces = maxFaces;
		}
	}

	public static class FSDK_Features {
		public TPoint features[] = new TPoint[FSDK_FACIAL_FEATURE_COUNT];
	}

	public static class FSDK_FaceTemplate {
		public byte template[] = new byte[2068];
	}

	public static class IDSimilarity {
		public long ID;
		public float similarity;
	}


	// Facial features

	public static final int FSDKP_LEFT_EYE = 0;
	public static final int FSDKP_RIGHT_EYE	= 1;
	public static final int FSDKP_LEFT_EYE_INNER_CORNER =	24;
	public static final int FSDKP_LEFT_EYE_OUTER_CORNER =	23;
	public static final int FSDKP_LEFT_EYE_LOWER_LINE1 =	38;
	public static final int FSDKP_LEFT_EYE_LOWER_LINE2 =	27;
	public static final int FSDKP_LEFT_EYE_LOWER_LINE3 =	37;
	public static final int FSDKP_LEFT_EYE_UPPER_LINE1 =	35;
	public static final int FSDKP_LEFT_EYE_UPPER_LINE2 =	28;
	public static final int FSDKP_LEFT_EYE_UPPER_LINE3 =	36;
	public static final int FSDKP_LEFT_EYE_LEFT_IRIS_CORNER =	29;
	public static final int FSDKP_LEFT_EYE_RIGHT_IRIS_CORNER =	30;
	public static final int FSDKP_RIGHT_EYE_INNER_CORNER =	25;
	public static final int FSDKP_RIGHT_EYE_OUTER_CORNER =	26;
	public static final int FSDKP_RIGHT_EYE_LOWER_LINE1 =	41;
	public static final int FSDKP_RIGHT_EYE_LOWER_LINE2 =	31;
	public static final int FSDKP_RIGHT_EYE_LOWER_LINE3 =	42;
	public static final int FSDKP_RIGHT_EYE_UPPER_LINE1 =	40;
	public static final int FSDKP_RIGHT_EYE_UPPER_LINE2 =	32;
	public static final int FSDKP_RIGHT_EYE_UPPER_LINE3 =	39;
	public static final int FSDKP_RIGHT_EYE_LEFT_IRIS_CORNER =	33;
	public static final int FSDKP_RIGHT_EYE_RIGHT_IRIS_CORNER =	34;
	public static final int FSDKP_LEFT_EYEBROW_INNER_CORNER	 = 13;
	public static final int FSDKP_LEFT_EYEBROW_MIDDLE =	16;
	public static final int FSDKP_LEFT_EYEBROW_MIDDLE_LEFT =	18;
	public static final int FSDKP_LEFT_EYEBROW_MIDDLE_RIGHT	= 19;
	public static final int FSDKP_LEFT_EYEBROW_OUTER_CORNER	= 12;
	public static final int FSDKP_RIGHT_EYEBROW_INNER_CORNER =	14;
	public static final int FSDKP_RIGHT_EYEBROW_MIDDLE =	17;
	public static final int FSDKP_RIGHT_EYEBROW_MIDDLE_LEFT =	20;
	public static final int FSDKP_RIGHT_EYEBROW_MIDDLE_RIGHT =	21;
	public static final int FSDKP_RIGHT_EYEBROW_OUTER_CORNER =	15;
	public static final int FSDKP_NOSE_TIP =	2;
	public static final int FSDKP_NOSE_BOTTOM =	49;
	public static final int FSDKP_NOSE_BRIDGE =	22;
	public static final int FSDKP_NOSE_LEFT_WING =	43;
	public static final int FSDKP_NOSE_LEFT_WING_OUTER =	45;
	public static final int FSDKP_NOSE_LEFT_WING_LOWER =	47;
	public static final int FSDKP_NOSE_RIGHT_WING =	44;
	public static final int FSDKP_NOSE_RIGHT_WING_OUTER =	46;
	public static final int FSDKP_NOSE_RIGHT_WING_LOWER =	48;
	public static final int FSDKP_MOUTH_RIGHT_CORNER =	3;
	public static final int FSDKP_MOUTH_LEFT_CORNER	= 4;
	public static final int FSDKP_MOUTH_TOP	= 54;
	public static final int FSDKP_MOUTH_TOP_INNER	= 61;
	public static final int FSDKP_MOUTH_BOTTOM =	55;
	public static final int FSDKP_MOUTH_BOTTOM_INNER =	64;
	public static final int FSDKP_MOUTH_LEFT_TOP =	56;
	public static final int FSDKP_MOUTH_LEFT_TOP_INNER =	60;
	public static final int FSDKP_MOUTH_RIGHT_TOP =	57;
	public static final int FSDKP_MOUTH_RIGHT_TOP_INNER =	62;
	public static final int FSDKP_MOUTH_LEFT_BOTTOM =	58;
	public static final int FSDKP_MOUTH_LEFT_BOTTOM_INNER =	63;
	public static final int FSDKP_MOUTH_RIGHT_BOTTOM =	59;
	public static final int FSDKP_MOUTH_RIGHT_BOTTOM_INNER =	65;
	public static final int FSDKP_NASOLABIAL_FOLD_LEFT_UPPER =	50;
	public static final int FSDKP_NASOLABIAL_FOLD_LEFT_LOWER =	52;
	public static final int FSDKP_NASOLABIAL_FOLD_RIGHT_UPPER =	51;
	public static final int FSDKP_NASOLABIAL_FOLD_RIGHT_LOWER =	53;
	public static final int FSDKP_CHIN_BOTTOM =	11;
	public static final int FSDKP_CHIN_LEFT =	9;
	public static final int FSDKP_CHIN_RIGHT =	10;
	public static final int FSDKP_FACE_CONTOUR1 =	7;
	public static final int FSDKP_FACE_CONTOUR2 =	5;
	public static final int FSDKP_FACE_CONTOUR12 =	6;
	public static final int FSDKP_FACE_CONTOUR13 =	8;
	public static final int FSDKP_FACE_CONTOUR14 =	66;
	public static final int FSDKP_FACE_CONTOUR15 =	67;
	public static final int FSDKP_FACE_CONTOUR16 =	68;
	public static final int FSDKP_FACE_CONTOUR17 =	69;



	public static native int ActivateLibrary(String LicenseKey);
	//public static native int GetHardware_ID(String HardwareID[]); //not implemented
	public static native int GetLicenseInfo(String LicenseInfo[]);
	public static native int SetNumThreads(int Num);
	public static native int GetNumThreads(int Num[]);
	public static native int Initialize();
	public static native int Finalize();

	public static native int CreateEmptyImage(HImage Image);
	public static native int FreeImage(HImage Image);

	public static native int LoadImageFromFile(HImage Image, String FileName);
	public static native int LoadImageFromFileWithAlpha(HImage Image, String FileName);
	public static native int SaveImageToFile(HImage Image, String FileName);
	public static native int SetJpegCompressionQuality(int Quality);
	public static native int GetImageWidth(HImage Image, int Width[]);
	public static native int GetImageHeight(HImage Image, int Height[]);
	public static native int LoadImageFromBuffer(HImage Image, byte Buffer[], int Width, int Height, int ScanLine, FSDK_IMAGEMODE ImageMode);
	public static native int GetImageBufferSize(HImage Image, int BufSize [], FSDK_IMAGEMODE ImageMode);
	public static native int SaveImageToBuffer(HImage Image, byte Buffer[], FSDK_IMAGEMODE ImageMode);
	public static native int LoadImageFromJpegBuffer(HImage Image, byte Buffer[], int BufferLength);
	public static native int LoadImageFromPngBuffer(HImage Image, byte Buffer[], int BufferLength);
	public static native int LoadImageFromPngBufferWithAlpha(HImage Image, byte Buffer[], int BufferLength);

	public static native int DetectFace(HImage Image, TFacePosition FacePosition);
	public static native int DetectMultipleFaces(HImage Image, TFaces FacePositions);
	public static native int SetFaceDetectionParameters(boolean HandleArbitraryRotations, boolean DetermineFaceRotationAngle, int InternalResizeWidth);
	public static native int SetFaceDetectionThreshold(int Threshold);
	public static native int GetDetectedFaceConfidence(int Confidence[]);

	public static native int DetectFace2(HImage Image, TFace face);
	public static native int DetectMultipleFaces2(HImage Image, TFaces2 faces);

	public static native int DetectFacialFeatures(HImage Image, FSDK_Features FacialFeatures);
	public static native int DetectFacialFeaturesInRegion(HImage Image, TFacePosition FacePosition, FSDK_Features FacialFeatures);
	public static native int DetectEyes(HImage Image, FSDK_Features Eyes);
	public static native int DetectEyesInRegion(HImage Image, TFacePosition FacePosition, FSDK_Features Eyes);


	public static native int CopyImage(HImage SourceImage, HImage DestImage);
	public static native int ResizeImage(HImage SourceImage, double ratio, HImage DestImage);
	public static native int RotateImage90(HImage SourceImage, int Multiplier, HImage DestImage);
	public static native int RotateImage(HImage SourceImage, double angle, HImage DestImage);
	public static native int RotateImageCenter(HImage SourceImage, double angle, double xCenter, double yCenter, HImage DestImage);
	public static native int CopyRect(HImage SourceImage, int x1, int y1, int x2, int y2, HImage DestImage);
	public static native int CopyRectReplicateBorder(HImage SourceImage, int x1, int y1, int x2, int y2, HImage DestImage);
	public static native int MirrorImage(HImage Image, boolean UseVerticalMirroringInsteadOfHorizontal);

	public static native int ExtractFaceImage(HImage Image, FSDK_Features FacialFeatures, int Width, int Height, HImage ExtractedFaceImage, FSDK_Features ResizedFeatures);


	public static native int GetFaceTemplate(HImage Image, FSDK_FaceTemplate FaceTemplate);
	public static native int GetFaceTemplateInRegion(HImage Image, TFacePosition FacePosition, FSDK_FaceTemplate FaceTemplate);
	public static native int GetFaceTemplateUsingFeatures(HImage Image, FSDK_Features FacialFeatures, FSDK_FaceTemplate FaceTemplate);
	public static native int GetFaceTemplateUsingEyes(HImage Image, FSDK_Features EyeCoords, FSDK_FaceTemplate FaceTemplate);
	public static native int MatchFaces(FSDK_FaceTemplate FaceTemplate1, FSDK_FaceTemplate FaceTemplate2, float Similarity[]);
	public static native int GetMatchingThresholdAtFAR(float FARValue, float Threshold[]);
	public static native int GetMatchingThresholdAtFRR(float FRRValue, float Threshold[]);

	public static native int GetFaceTemplate2(HImage image, FSDK_FaceTemplate FaceTemplate);
	public static native int GetFaceTemplateInRegion2(HImage image, TFace face, FSDK_FaceTemplate FaceTemplate);

	public static native int CreateTracker(HTracker Tracker);
	public static native int FreeTracker(HTracker Tracker);
	public static native int ClearTracker(HTracker Tracker);
	public static native int SetTrackerParameter(HTracker Tracker, String ParameterName, String ParameterValue);
	public static native int SetTrackerMultipleParameters(HTracker Tracker, String Parameters, int ErrorPosition[]);
	public static native int GetTrackerParameter(HTracker Tracker, String ParameterName, String ParameterValue[], int MaxSizeInBytes);
	public static native int FeedFrame(HTracker Tracker, long CameraIdx, HImage Image, long FaceCount[], long IDs[]);
	public static native int GetTrackerEyes(HTracker Tracker, long CameraIdx, long ID, FSDK_Features Eyes);
	public static native int GetTrackerFacialFeatures(HTracker Tracker, long CameraIdx, long ID, FSDK_Features FacialFeatures);
	public static native int GetTrackerFacePosition(HTracker Tracker, long CameraIdx, long ID, TFacePosition FacePosition);
	public static native int GetTrackerFace(HTracker Tracker, long CameraIdx, long ID, TFace Face);
	public static native int LockID(HTracker Tracker, long ID);
	public static native int UnlockID(HTracker Tracker, long ID);
	public static native int PurgeID(HTracker Tracker, long ID);
	public static native int SetName(HTracker Tracker, long ID, String Name);
	public static native int GetName(HTracker Tracker, long ID, String Name[], long MaxSizeInBytes);
	public static native int GetAllNames(HTracker Tracker, long ID, String Names[], long MaxSizeInBytes);
	public static native int GetIDReassignment(HTracker Tracker, long ID, long ReassignedID[]);
	public static native int GetSimilarIDCount(HTracker Tracker, long ID, long Count[]);
	public static native int GetSimilarIDList(HTracker Tracker, long ID, long SimilarIDList[]);

	public static native int SaveTrackerMemoryToFile(HTracker Tracker, String FileName);
	public static native int LoadTrackerMemoryFromFile(HTracker Tracker, String FileName);
	public static native int GetTrackerMemoryBufferSize(HTracker Tracker, long [] BufSize);
	public static native int SaveTrackerMemoryToBuffer(HTracker Tracker, byte Buffer[]);
	public static native int LoadTrackerMemoryFromBuffer(HTracker Tracker, byte Buffer[]);

	public static native int GetTrackerIDsCount(HTracker tracker, long[] Count);
	public static native int GetTrackerAllIDs(HTracker tracker, long[] IDList);
	public static native int GetTrackerFaceIDsCountForID(HTracker tracker, long ID, long[] Count);
	public static native int GetTrackerFaceIDsForID(HTracker tracker, long ID, long[] FaceIDList);
	public static native int GetTrackerIDByFaceID(HTracker tracker, long FaceID, long[] ID);
	public static native int GetTrackerFaceTemplate(HTracker tracker, long FaceID, FSDK_FaceTemplate FaceTemplate);
	public static native int GetTrackerFaceImage(HTracker tracker, long FaceID, HImage Image);
	public static native int SetTrackerFaceImage(HTracker tracker, long FaceID, HImage Image);
	public static native int DeleteTrackerFaceImage(HTracker tracker, long FaceID);
	public static native int TrackerCreateID(HTracker tracker, FSDK_FaceTemplate FaceTemplate, long[] ID, long[] FaceID);
	public static native int AddTrackerFaceTemplate(HTracker tracker, long ID, FSDK_FaceTemplate FaceTemplate, long[] FaceID);
	public static native int TrackerMatchFaces(HTracker tracker, FSDK_FaceTemplate FaceTemplate, float Threshold, IDSimilarity[] Buffer, long[] Count);

	public static native int GetTrackerFacialAttribute(HTracker Tracker, long CameraIdx, long ID, String AttributeName, String AttributeValues[], long MaxSizeInBytes);
	public static native int DetectFacialAttributeUsingFeatures(HImage Image, FSDK_Features FacialFeatures, String AttributeName, String AttributeValues[], long MaxSizeInBytes);
	public static native int GetValueConfidence(String AttributeValues, String Value, float Confidence[]);

	public static native int SetHTTPProxy(String ServerNameOrIPAddress, short Port, String UserName, String Password);
	public static native int OpenIPVideoCamera(FSDK_VIDEOCOMPRESSIONTYPE CompressionType, String URL, String Username, String Password, int TimeoutSeconds, HCamera CameraHandle);
	public static native int CloseVideoCamera(HCamera CameraHandle);
	public static native int GrabFrame(HCamera CameraHandle, HImage Image);
	public static native int InitializeCapturing();
	public static native int FinalizeCapturing();

	public static native int SetParameter(String ParameterName, String ParameterValue);
	public static native int SetParameters(String Parameters, int ErrorPosition[]);

	public static int InitializeLibrary() {
		return Initialize();
	}

	private static class AssetsHelper {
		private final String assetsPath;
		private final AssetManager assetManager;

		public AssetsHelper(final Application app) {
			assetManager = app.getAssets();
			assetsPath = app.getApplicationContext().getCacheDir().getAbsolutePath() + "/";
		}

		private void copyFile(final String filename) {
			try {
				final var in = assetManager.open(filename);

				final var newFileName = assetsPath + filename;
				final var out = new FileOutputStream(newFileName);

				final var buffer = new byte[1024];

				int read;
				while ((read = in.read(buffer)) != -1)
					out.write(buffer, 0, read);

				in.close();
				out.flush();
				out.close();
			} catch (Exception e) {
				var message = e.getMessage();
				if (message == null)
					message = "Copying Asset Error";
				Log.e("luxand_fsdk", message);
			}
		}

		public void copyFileOrDir(final String path)
		{
			try {
				final var assets = assetManager.list(path);
				if (assets == null) {
					Log.e("luxand_fsdk", "Could not list assets for " + path);
					return;
				}

				if (assets.length == 0) {
					copyFile(path);
					return;
				}

				final var dir = new File(assetsPath + path);
				if (!dir.exists() && !dir.mkdir()) {
					Log.e("luxand_fsdk", "Error creating directory for assets " + dir.getAbsolutePath());
					return;
				}

                for (final var asset : assets)
                    copyFileOrDir(path + "/" + asset);

			} catch (IOException ex) {
				Log.e("luxand_fsdk", "I/O Exception", ex);
			}
		}
	};

	public static int PrepareData(final Application app)
	{
		new AssetsHelper(app).copyFileOrDir("data");
		return 0;
	}

}
