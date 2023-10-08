/*============================================================================== 
 * Copyright (c) 2015 Qualcomm Connected Experiences, Inc. All Rights Reserved. 
 * ==============================================================================*/
using UnityEngine;
using UnityEngine.UI;
using System.Collections;
using System.Collections.Generic;
using System.Linq;
using Vuforia;

public class UDTEventHandler : MonoBehaviour, IUserDefinedTargetEventHandler
{
    #region PUBLIC_MEMBERS
    /// <summary>
    /// Can be set in the Unity inspector to reference a ImageTargetBehaviour that is instanciated for augmentations of new user defined targets.
    /// </summary>
    public ImageTargetBehaviour ImageTargetTemplate;

    public int LastTargetIndex {
        get { return (mTargetCounter - 1) % MAX_TARGETS; }
    }
    #endregion PUBLIC_MEMBERS


    #region PRIVATE_MEMBERS
    private const int MAX_TARGETS = 5;
    private UserDefinedTargetBuildingBehaviour mTargetBuildingBehaviour;
    private QualityDialog mQualityDialog;
    private ObjectTracker mObjectTracker;
    
    // DataSet that newly defined targets are added to
    private DataSet mBuiltDataSet;
    
    // Currently observed frame quality
    private ImageTargetBuilder.FrameQuality mFrameQuality = ImageTargetBuilder.FrameQuality.FRAME_QUALITY_NONE;
    
    // Counter used to name newly created targets
    private int mTargetCounter;

    private TrackableSettings mTrackableSettings;
    #endregion //PRIVATE_MEMBERS


    #region MONOBEHAVIOUR_METHODS

    private bool mFormatRegistered = false;
    private bool mAccessCameraImmage = true;
    private Vuforia.Image.PIXEL_FORMAT mPixelFormat = Vuforia.Image.PIXEL_FORMAT.GRAYSCALE;

    public void test(string toPrint)
    {
        System.Console.WriteLine("{0}", toPrint);
        Debug.Log("Accessins test method from inside Android Studios");
        Debug.Log(toPrint);
    }
    public void Start()
    {

        //VuforiaBehaviour.Instance.RegisterVuforiaStartedCallback(OnVuforiaStarted);      
        //VuforiaBehaviour.Instance.RegisterOnPauseCallback(OnPause);
        //VuforiaBehaviour.Instance.RegisterTrackablesUpdatedCallback(OnTrackablesUpdated);

        mTargetBuildingBehaviour = GetComponent<UserDefinedTargetBuildingBehaviour>();
        if (mTargetBuildingBehaviour)
        {
            mTargetBuildingBehaviour.RegisterEventHandler(this);
            Debug.Log("Registering User Defined Target event handler.");
        }

        mTrackableSettings = FindObjectOfType<TrackableSettings>();
        mQualityDialog = FindObjectOfType<QualityDialog>();
        if (mQualityDialog)
        {
            mQualityDialog.gameObject.SetActive(false);
        }
    }
    private void OnPause(bool paused)
    {
        if (paused)
        {
            Debug.Log("App was paused");
            UnregisterFormat();
        }

        else
        {
            Debug.Log("App was resumed");
            RegisterFormat();
        }
    }
    private void OnVuforiaStarted()
    {
        if (CameraDevice.Instance.SetFrameFormat(mPixelFormat, true))
        {
            Debug.Log("Successfully registered pixel format " + mPixelFormat.ToString());
            mFormatRegistered = true;
        }
        else
        {
            Debug.LogError("Failed to register pixel format " + mPixelFormat.ToString() +
    "\n the format may be unsupported by your device;" +
    "\n consider using a different pixel format.");
            mFormatRegistered = false;
        }
    }

    private void OnTrackablesUpdated()
    {
        //Debug.Log("OnTrackablesUpdated method");
        if (mFormatRegistered)
        {
            mAccessCameraImmage = false;
            //Debug.Log("Format Registered");
            if (mAccessCameraImmage)
            {
                //Debug.Log("Camera Image");
                Vuforia.Image image = CameraDevice.Instance.GetCameraImage(mPixelFormat);
                if (image != null)
                {
                    string imageInfo = mPixelFormat + " image: \n";
                    imageInfo += " size: " + image.Width + " x " + image.Height + "\n";
                    imageInfo += " bufferSize: " + image.BufferWidth + " x " + image.BufferHeight + "\n";
                    imageInfo += " stride: " + image.Stride;
                    Debug.Log(imageInfo);
                    byte[] pixels = image.Pixels;
                    if (pixels != null && pixels.Length > 0)
                    {
                        Debug.Log("Image pixels: " + pixels[0] + "," + pixels[1] + "," + pixels[2] + ",...");
                    }
                }
            }
        }
    }
    /// <summary>
    /// Unregister the camera pixel format (e.g. call this when app is paused)
    /// </summary>
    private void UnregisterFormat()
    {
        Debug.Log("Unregistering camera pixel format " + mPixelFormat.ToString());
        CameraDevice.Instance.SetFrameFormat(mPixelFormat, false);
        mFormatRegistered = false;
    }
    /// <summary>
    /// Register the camera pixel format
    /// </summary>
    private void RegisterFormat()
    {
        if (CameraDevice.Instance.SetFrameFormat(mPixelFormat, true))
        {
            Debug.Log("Successfully registered camera pixel format " + mPixelFormat.ToString());
            mFormatRegistered = true;
        }
        else
        {
            Debug.LogError("Failed to register camera pixel format " + mPixelFormat.ToString());
            mFormatRegistered = false;
        }
    }
    #endregion //MONOBEHAVIOUR_METHODS


    #region IUserDefinedTargetEventHandler implementation
    /// <summary>
    /// Called when UserDefinedTargetBuildingBehaviour has been initialized successfully
    /// </summary>
    public void OnInitialized()
    {
        mObjectTracker = TrackerManager.Instance.GetTracker<ObjectTracker>();
        if (mObjectTracker != null)
        {
            // Create a new dataset
            mBuiltDataSet = mObjectTracker.CreateDataSet();
            mObjectTracker.ActivateDataSet(mBuiltDataSet);
        }
    }

    /// <summary>
    /// Updates the current frame quality
    /// </summary>
    public void OnFrameQualityChanged(ImageTargetBuilder.FrameQuality frameQuality)
    {
        //Debug.Log("Frame quality changed: " + frameQuality.ToString());
        mFrameQuality = frameQuality;
        if (mFrameQuality == ImageTargetBuilder.FrameQuality.FRAME_QUALITY_LOW)
        {
            //Debug.Log("Low camera image quality");
        }
        
        //Have this do nothing.
    }

    /// <summary>
    /// Takes a new trackable source and adds it to the dataset
    /// This gets called automatically as soon as you 'BuildNewTarget with UserDefinedTargetBuildingBehaviour
    /// </summary>
    public void OnNewTrackableSource(TrackableSource trackableSource)
    {
        mTargetCounter++;
        
        // Deactivates the dataset first
        mObjectTracker.DeactivateDataSet(mBuiltDataSet);

        // Destroy the oldest target if the dataset is full or the dataset 
        // already contains five user-defined targets.
        if (mBuiltDataSet.HasReachedTrackableLimit() || mBuiltDataSet.GetTrackables().Count() >= MAX_TARGETS)
        {
            IEnumerable<Trackable> trackables = mBuiltDataSet.GetTrackables();
            Trackable oldest = null;
            foreach (Trackable trackable in trackables)
            {
                if (oldest == null || trackable.ID < oldest.ID)
                    oldest = trackable;
            }
            
            if (oldest != null)
            {
                Debug.Log("Destroying oldest trackable in UDT dataset: " + oldest.Name);
                mBuiltDataSet.Destroy(oldest, true);
            }
        }
        
        // Get predefined trackable and instantiate it
        ImageTargetBehaviour imageTargetCopy = (ImageTargetBehaviour)Instantiate(ImageTargetTemplate);
        imageTargetCopy.gameObject.name = "UserDefinedTarget-" + mTargetCounter;
        
        // Add the duplicated trackable to the data set and activate it
        mBuiltDataSet.CreateTrackable(trackableSource, imageTargetCopy.gameObject);
        //Debug.Log(imageTargetCopy.name);
        // Activate the dataset again
        mObjectTracker.ActivateDataSet(mBuiltDataSet);

        // Extended Tracking with user defined targets only works with the most recently defined target.
        // If tracking is enabled on previous target, it will not work on newly defined target.
        // Don't need to call this if you don't care about extended tracking.
        StopExtendedTracking();
        mObjectTracker.Stop();
        mObjectTracker.ResetExtendedTracking();
        mObjectTracker.Start();

        // Make sure TargetBuildingBehaviour keeps scanning...
        mTargetBuildingBehaviour.StartScanning();
    }
    #endregion IUserDefinedTargetEventHandler implementation


    #region PUBLIC_METHODS
    /// <summary>
    /// Instantiates a new user-defined target and is also responsible for dispatching callback to 
    /// IUserDefinedTargetEventHandler::OnNewTrackableSource
    /// </summary>
    public void BuildNewTarget()
    {
        if (mFrameQuality == ImageTargetBuilder.FrameQuality.FRAME_QUALITY_MEDIUM || 
            mFrameQuality == ImageTargetBuilder.FrameQuality.FRAME_QUALITY_HIGH)
        {
            // create the name of the next target.
            // the TrackableName of the original, linked ImageTargetBehaviour is extended with a continuous number to ensure unique names
            string targetName = string.Format("{0}-{1}", ImageTargetTemplate.TrackableName, mTargetCounter);

            // generate a new target:
            mTargetBuildingBehaviour.BuildNewTarget(targetName, ImageTargetTemplate.GetSize().x);
        }
        else
        {
            Debug.Log("Cannot build new target, due to poor camera image quality");
            if (mQualityDialog)
            {
                mQualityDialog.gameObject.SetActive(true);
            }
        }
    }
    public void BuildTorg()
    {
        if (mFrameQuality == ImageTargetBuilder.FrameQuality.FRAME_QUALITY_MEDIUM ||
            mFrameQuality == ImageTargetBuilder.FrameQuality.FRAME_QUALITY_HIGH)
        {
            // create the name of the next target.
            // the TrackableName of the original, linked ImageTargetBehaviour is extended with a continuous number to ensure unique names
            string targetName = string.Format("torgersen-{0}", mTargetCounter);

            // generate a new target:
            mTargetBuildingBehaviour.BuildNewTarget(targetName, ImageTargetTemplate.GetSize().x);
        }
        else
        {
            Debug.Log("Cannot build new target, due to poor camera image quality");
            if (mQualityDialog)
            {
                mQualityDialog.gameObject.SetActive(true);
            }
        }
    }
    public void BuildNewman()
    {
        if (mFrameQuality == ImageTargetBuilder.FrameQuality.FRAME_QUALITY_MEDIUM ||
            mFrameQuality == ImageTargetBuilder.FrameQuality.FRAME_QUALITY_HIGH)
        {
            // create the name of the next target.
            // the TrackableName of the original, linked ImageTargetBehaviour is extended with a continuous number to ensure unique names
            string targetName = string.Format("newman-{0}", mTargetCounter);

            // generate a new target:
            mTargetBuildingBehaviour.BuildNewTarget(targetName, ImageTargetTemplate.GetSize().x);
        }
        else
        {
            Debug.Log("Cannot build new target, due to poor camera image quality");
            if (mQualityDialog)
            {
                mQualityDialog.gameObject.SetActive(true);
            }
        }
    }

    public void CloseQualityDialog()
    {
        if (mQualityDialog)
            mQualityDialog.gameObject.SetActive(false);
    }
    #endregion //PUBLIC_METHODS


    #region PRIVATE_METHODS
    /// <summary>
    /// This method only demonstrates how to handle extended tracking feature when you have multiple targets in the scene
    /// So, this method could be removed otherwise
    /// </summary>
    private void StopExtendedTracking()
    {
        // If Extended Tracking is enabled, we first disable it for all the trackables
        // and then enable it only for the newly created target
        bool extTrackingEnabled = mTrackableSettings && mTrackableSettings.IsExtendedTrackingEnabled();
        if (extTrackingEnabled)
        {
            StateManager stateManager = TrackerManager.Instance.GetStateManager();

            // 1. Stop extended tracking on all the trackables
            foreach (var tb in stateManager.GetTrackableBehaviours())
            {
                var itb = tb as ImageTargetBehaviour;
                if (itb != null)
                {
                    itb.ImageTarget.StopExtendedTracking();
                }
            }
    
            // 2. Start Extended Tracking on the most recently added target
            List<TrackableBehaviour> trackableList = stateManager.GetTrackableBehaviours().ToList();
            ImageTargetBehaviour lastItb = trackableList[LastTargetIndex] as ImageTargetBehaviour;
            if (lastItb != null)
            {
                if (lastItb.ImageTarget.StartExtendedTracking())
                    Debug.Log("Extended Tracking successfully enabled for " + lastItb.name);
            }
        }
    }
    #endregion //PRIVATE_METHODS
}



