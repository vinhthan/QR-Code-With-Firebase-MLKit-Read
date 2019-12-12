package com.example.qrcodewithfirebasemlreadll1;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.frame.Frame;
import com.otaliastudios.cameraview.frame.FrameProcessor;

import java.util.List;

//quet thong tin ca nhan:
//https://sites.google.com/site/nguyenthibaongoc999999/gioi-thieu/thong-tin-ca-nhan


public class MainActivity extends AppCompatActivity {

    CameraView cameraView;
    Button btnStartAgain;
    boolean isDetected = false;

    FirebaseVisionBarcodeDetectorOptions options;
    FirebaseVisionBarcodeDetector detector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Dexter.withActivity(this)
                .withPermissions(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        setupCamera();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {

                    }
                }).check();
    }

    private void setupCamera() {
        btnStartAgain = findViewById(R.id.btnAgain);
        btnStartAgain.setEnabled(isDetected);
        btnStartAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isDetected = !isDetected;//chua dc phat hien de quet lai
                btnStartAgain.setEnabled(isDetected);
            }
        });

        cameraView = findViewById(R.id.cameraView);
        cameraView.setLifecycleOwner(this);
        cameraView.addFrameProcessor(new FrameProcessor() {
            @Override
            public void process(@NonNull Frame frame) {
                processImage(getVisitionImageFromFrame(frame));
            }
        });

        options = new FirebaseVisionBarcodeDetectorOptions.Builder()
                .setBarcodeFormats(FirebaseVisionBarcode.FORMAT_QR_CODE)
                .build();
        detector = FirebaseVision.getInstance().getVisionBarcodeDetector(options);

    }

    private void processImage(FirebaseVisionImage image) {
        if (!isDetected) {
            detector.detectInImage(image)
                    .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionBarcode>>() {
                        @Override
                        public void onSuccess(List<FirebaseVisionBarcode> firebaseVisionBarcodes) {
                            processResult(firebaseVisionBarcodes);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(MainActivity.this, "", Toast.LENGTH_SHORT).show();
                        }
                    });
        }

    }

    private void processResult(List<FirebaseVisionBarcode> firebaseVisionBarcodes) {
        if (firebaseVisionBarcodes.size() > 0) {
            isDetected = true;
            btnStartAgain.setEnabled(isDetected);

            for (FirebaseVisionBarcode item : firebaseVisionBarcodes) {
                int valueType = item.getValueType();
                switch (valueType) {

                    //read text
                    case FirebaseVisionBarcode.TYPE_TEXT: {
                        createdDialog(item.getRawValue());
                    }
                    finish();
                    break;

                    //read url
                    case FirebaseVisionBarcode.TYPE_URL:{
                        // start brower intent
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(item.getRawValue()));
                        //tránh bị lỗi mở quá nhiều tab lặp lại trên trình duyệt
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }
                    break;

                    //read vcard
                    case FirebaseVisionBarcode.TYPE_CONTACT_INFO:{
                        String into = new StringBuilder("-Title: ")
                                .append(item.getContactInfo().getTitle())
                                .append("\n\n")
                                .append("-Name: ")
                                .append(item.getContactInfo().getName().getFormattedName())
                                .append("\n")
                                .append("-Email: ")
                                .append(item.getContactInfo().getEmails().get(0).getAddress())
                                .append("\n")
                                .append("-Phone: ")
                                .append(item.getContactInfo().getPhones().get(0).getNumber())
                                .append("\n")
                                .append("-Address: ")
                                .append(item.getContactInfo().getAddresses().get(0).getAddressLines()[0])
                                .append("\n")
                                .append("-Country: ")
                                .append(item.getContactInfo().getOrganization())
                                /*.append("\n")
                                .append("Geo point")
                                .append(item.getGeoPoint()...)*/
                                .toString();
                        createdDialog(into);

                    }
                    break;

                    //read more...
                    default:
                        break;
                }
            }
        }
    }

    private FirebaseVisionImage getVisitionImageFromFrame(Frame frame) {

        byte[] data = frame.getData();
        FirebaseVisionImageMetadata metadata = new FirebaseVisionImageMetadata.Builder()
                .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
                .setHeight(frame.getSize().getHeight())
                .setWidth(frame.getSize().getWidth())
                //.setRotation(frame.getRotation())//only use it if you want to work on land scape
                .build();
        return FirebaseVisionImage.fromByteArray(data, metadata);
    }

    private void createdDialog(String rawValue) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(rawValue)
                .setMessage(rawValue)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .setNegativeButton("exit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

}
