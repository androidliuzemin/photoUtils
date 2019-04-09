package com.banban.kuxiu.utils;

import android.Manifest;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.util.TypedValue;

import com.banban.kuxiu.MyApplication;
import com.banban.kuxiu.base.BaseActivity;
import com.banban.kuxiu.framework.util.ToastUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class PhotoUtils {
    private static final int SIZE_LIMIT = 1080;
    private String cameraImgPath = "";//拍照之后的照片存储路径
    private Uri cameraUri;//拍照之后的照片uri
    private Uri cropUri;//剪裁之后的照片uri


    public String getCameraPath() {
        return cameraImgPath;
    }

    /**
     * 获取最终剪裁好的图片路径
     *
     * @return
     */
    public String getCropPath() {
        return cropUri == null ? "" : cropUri.getPath();
    }

    /**
     * 创建拍照照片保存路径
     */
    public static String createCameraFile(Context context) {
        String path = "";
        File file = null;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            file = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + "cameraPic");
        } else {
            file = new File(context.getFilesDir().getPath() + File.separator + "cameraPic");
        }
        if (file != null) {
            if (!file.exists()) {
                file.mkdir();
            }
            File output = new File(file, System.currentTimeMillis() + ".png");
            try {
                if (output.exists()) {
                    output.delete();
                } else {
                    output.createNewFile();
                }
                path = output.getAbsolutePath();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return path;
    }

    /**
     * 创建拍照照片保存路径Uri
     */
    public Uri createCameraUri(Context context) {
        Uri uri = null;
        File file = null;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            file = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + "cameraPic");
        } else {
            file = new File(context.getFilesDir().getPath() + File.separator + "cameraPic");
        }
        if (file != null) {
            if (!file.exists()) {
                file.mkdirs();
            }
            File output = new File(file, System.currentTimeMillis() + ".png");
            try {
                if (output.exists()) {
                    output.delete();
                } else {
                    output.createNewFile();
                }
                if (Build.VERSION.SDK_INT >= 24) {
                    //android7.0以上，本地uri是不安全的，需要用provider
                    uri = FileProvider.getUriForFile(context, "com.banban.kuxiu.myPicProvider", output);
                } else {
                    uri = Uri.fromFile(output);
                }
                cameraImgPath = output.getAbsolutePath();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return uri;
    }

    /**
     * 创建剪裁照片保存路径Uri
     */
    public Uri createCropUri(Context context) {
        Uri uri = null;
        File file = null;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            file = new File(Environment.getExternalStorageDirectory().getPath()
                    + File.separator + "cameraPic" + File.separator + "cropPic");
        } else {
            file = new File(context.getFilesDir().getPath()
                    + File.separator + "cameraPic" + File.separator + "cropPic");
        }
        if (file != null) {
            if (!file.exists()) {
                file.mkdirs();
            }
            File output = new File(file.getAbsolutePath() + File.separator + "kuxiu"+System.currentTimeMillis() + ".png");
            try {
                if (output.exists()) {
                    output.delete();
                } else {
                    output.createNewFile();
                }
                uri = Uri.fromFile(output);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return uri;
    }

    /**
     * 打开相册
     */
    public void selectPic(Activity activity, int requestCode) {
        try {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (activity instanceof BaseActivity) {
                    ((BaseActivity) activity).PERMISSIONS = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
                    ((BaseActivity) activity).startPermissionsActivity();
                } else {
                    ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 102);
                }
            } else {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                //下面这种方式也可以，只是打开相册的展示样式不一样
//        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//        intent.setType("image/*");
                activity.startActivityForResult(intent, requestCode);
            }
        } catch (Exception e){
            ToastUtils.showShortToast(MyApplication.getApplication(), "当前应用缺少必要权限");
        }


    }

    /**
     * 打开照相机
     */
    public void openCamera(Activity activity, int requestCode) {
        try {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                if (activity instanceof BaseActivity) {
                    ((BaseActivity) activity).PERMISSIONS = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA};
                    ((BaseActivity) activity).startPermissionsActivity();
                } else {
                    ActivityCompat.requestPermissions(activity,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, 101);
                }
            } else {
                try {
                    cameraUri = createCameraUri(activity);
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
                    activity.startActivityForResult(intent, requestCode);
                } catch (Exception e) {
                    ToastUtils.showShortToast(MyApplication.getApplication(), "当前应用缺少必要权限");
                }

            }
        } catch (Exception e){
            ToastUtils.showShortToast(MyApplication.getApplication(), "当前应用缺少必要权限");
        }

    }

    /**
     * 打开剪裁界面
     *
     * @param activity
     * @param uri         需要剪裁的图片uri
     * @param width       剪裁之后输出图片宽
     * @param height      剪裁之后输出图片高
     * @param requestCode 请求码
     */
    public void cropImg(Activity activity, Uri uri, int width, int height, int requestCode) {
        cropUri = createCropUri(activity);
        Intent intent = new Intent("com.android.camera.action.CROP");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        intent.setDataAndType(uri, "image/*");
        //裁剪图片的宽高比例
        intent.putExtra("aspectX", width);
        intent.putExtra("aspectY", height);
        intent.putExtra("crop", "true");//可裁剪
        // 裁剪后输出图片的尺寸大小
        intent.putExtra("outputX", width);
        intent.putExtra("outputY", height);
        intent.putExtra("scale", true);//去黑边
        intent.putExtra("scaleUpIfNeeded", true);//去黑边
        intent.putExtra("return-data", false);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cropUri);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());//输出图片格式
        intent.putExtra("noFaceDetection", true);//取消人脸识别
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * 通过uri获取bitmap对象
     *
     * @param context
     * @return
     */
    public Bitmap getBitmapFromUri(Context context, Uri uri) {
        Bitmap bitmap = null;
        try {
            bitmap = MediaStore.Images.Media.
                    getBitmap(context.getContentResolver(), uri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    /**
     * 获取content类型的uri路径
     *
     * @param context
     * @param uri
     * @param selection
     * @return
     */
    public String getImagePath(Context context, Uri uri, String selection) {
        String[] filePathColumn = {MediaStore.Images.Media.DATA};
        Cursor cursor = context.getContentResolver().query(uri, filePathColumn, selection, null, null);
        cursor.moveToFirst();
        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        String picPath = cursor.getString(columnIndex);
        cursor.close();
        return picPath;
    }

    /**
     * 根据各种类型uri获取资源路径
     *
     * @param context
     * @param uri
     * @return
     */
    public String getPathFromUri(Context context, Uri uri) {
        String imagePath = "";
        if (Build.VERSION.SDK_INT >= 19) {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                // 如果是document类型的Uri，则通过document id处理
                String docId = DocumentsContract.getDocumentId(uri);
                if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                    String id = docId.split(":")[1]; // 解析出数字格式的id
                    String selection = MediaStore.Images.Media._ID + "=" + id;
                    //注意！这里第二个参数千万不能传上面传参进来的uri
                    imagePath = getImagePath(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
                } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                    Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                    imagePath = getImagePath(context, contentUri, null);
                }
            } else if ("content".equalsIgnoreCase(uri.getScheme())) {
                // 如果是content类型的Uri，则使用普通方式处理
                imagePath = getImagePath(context, uri, null);
            } else if ("file".equalsIgnoreCase(uri.getScheme())) {
                // 如果是file类型的Uri，直接获取图片路径即可
                imagePath = uri.getPath();
            }
        } else {
            imagePath = getImagePath(context, uri, null);
        }
        return imagePath;
    }

    /**
     * 保存Bitmap图片在SD卡中
     * 如果没有SD卡则存在手机中
     *
     * @param mbitmap 需要保存的Bitmap图片
     * @return 保存成功时返回图片的路径，失败时返回null
     */
    public String savePhotoToSD(Bitmap mbitmap, Context context) {
        FileOutputStream outStream = null;
        String fileName = createCameraFile(context);
        try {
            outStream = new FileOutputStream(fileName);
            // 把数据写入文件，100表示不压缩
            mbitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
            return fileName;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (outStream != null) {
                    // 记得要关闭流！
                    outStream.close();
                }
                if (mbitmap != null) {
                    mbitmap.recycle();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 处理旋转后的图片
     *
     * @param originpath 原图路径
     * @param context    上下文
     * @return 返回修复完毕后的图片路径
     */
    public String amendRotatePhoto(String originpath, Context context) {
        // 取得图片旋转角度
        int angle = readPictureDegree(originpath);
        // 把原图压缩后得到Bitmap对象
        Bitmap bmp = getCompressPhoto(originpath, 2);
        // 修复图片被旋转的角度
        Bitmap bitmap = rotaingImageView(angle, bmp);
        // 保存修复后的图片并返回保存后的图片路径
        return savePhotoToSD(bitmap, context);
    }

    /**
     * 解决有些品牌手机拍照后图片会自动旋转（例如：Samsung）
     *
     * @param context
     * @return
     */
    public Uri getCameraUri(Context context) {
        File file = new File(amendRotatePhoto(cameraImgPath, context));
        Uri mUri = null;
        if (Build.VERSION.SDK_INT >= 24) {
            //android7.0以上，本地uri是不安全的，需要用provider
            mUri = FileProvider.getUriForFile(context, "com.banban.kuxiu.myPicProvider", file);
        } else {
            mUri = Uri.fromFile(file);
        }
        return mUri;
    }

    /**
     * 读取照片旋转角度
     *
     * @param path 照片路径
     * @return 角度
     */
    public int readPictureDegree(String path) {
        int degree = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }

    /**
     * 旋转图片
     *
     * @param angle  被旋转角度
     * @param bitmap 图片对象
     * @return 旋转后的图片
     */
    public Bitmap rotaingImageView(int angle, Bitmap bitmap) {
        Bitmap returnBm = null;
        // 根据旋转角度，生成旋转矩阵
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        try {
            // 将原始图片按照旋转矩阵进行旋转，并得到新的图片
            returnBm = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch (OutOfMemoryError e) {

        }
        if (returnBm == null) {
            returnBm = bitmap;
        }
        if (bitmap != returnBm) {
            bitmap.recycle();
        }
        return returnBm;
    }

    /**
     * 把原图按比例压缩
     *
     * @param srcPath 原图的路径
     * @return 压缩后的图片
     */
    public Bitmap getCompressPhoto(String srcPath, int sampleSize) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        //开始读入图片，此时把options.inJustDecodeBounds 设回true了
        options.inJustDecodeBounds = true;
        Bitmap bitmap = BitmapFactory.decodeFile(srcPath, options);//此时返回bm为空

        options.inJustDecodeBounds = false;
        int maxSize = SIZE_LIMIT;

        if (options.outHeight > 0 && options.outWidth > 0) {
            while (options.outHeight / sampleSize > maxSize || options.outWidth / sampleSize > maxSize) {
                sampleSize = sampleSize + 1;
            }
        } else {
            sampleSize = sampleSize << 1;
        }
        options.inSampleSize = sampleSize;//设置缩放比例
        try {
            //重新读入图片，注意此时已经把options.inJustDecodeBounds 设回false了
            bitmap = BitmapFactory.decodeFile(srcPath, options);
        } catch (OutOfMemoryError e) {
            options.inSampleSize *= 2;
            bitmap = BitmapFactory.decodeFile(srcPath, options);
        }
        return bitmap;
    }

    /**
     * 质量压缩方法
     *
     * @param
     * @return
     */
    public String compressReSave(String srcPath, Context context) {
        String filePath = "";
        Bitmap image = BitmapFactory.decodeFile(srcPath);
        if (image == null){
            return "";
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);// 质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
        int options = 90;
        while (baos.toByteArray().length / 1024 > 300) { // 循环判断如果压缩后图片是否大于300kb,大于继续压缩
            baos.reset(); // 重置baos即清空baos
            image.compress(Bitmap.CompressFormat.JPEG, options, baos);// 这里压缩options%，把压缩后的数据存放到baos中
            if (options > 10){
                options -= 10;// 每次都减少10
            } else {
                options -= 5;
            }

            if (options == 5){
                break;
            }
        }
        FileOutputStream outStream = null;
        filePath = createCameraFile(context);
        try {
            outStream = new FileOutputStream(filePath);
            // 把数据写入文件
            outStream.write(baos.toByteArray());
            // 记得要关闭流！
            outStream.close();
            return filePath;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (outStream != null) {
                    // 记得要关闭流！
                    outStream.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 质量压缩方法
     *
     * @param
     * @return
     */
    public String compressReSave2(String srcPath, Context context) {
        String filePath = "";
        Bitmap image = getCompressPhoto(srcPath, 2);
        if (image == null){
            return "";
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);// 质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
        int options = 90;
        while (baos.toByteArray().length / 1024 > 300) { // 循环判断如果压缩后图片是否大于300kb,大于继续压缩
            baos.reset(); // 重置baos即清空baos
            image.compress(Bitmap.CompressFormat.JPEG, options, baos);// 这里压缩options%，把压缩后的数据存放到baos中
            if (options > 10){
                options -= 10;// 每次都减少10
            } else {
                options -= 5;
            }

            if (options == 5){
                break;
            }
        }
        FileOutputStream outStream = null;
        filePath = createCameraFile(context);
        try {
            outStream = new FileOutputStream(filePath);
            // 把数据写入文件
            outStream.write(baos.toByteArray());
            // 记得要关闭流！
            outStream.close();
            return filePath;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (outStream != null) {
                    // 记得要关闭流！
                    outStream.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
