package com.nio.ocr.ektp.mobile;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.regex.Pattern;

public class Constants {
    public static final String TAG = "OCReKTP";
    static final String BLACKLIST_CHARS = "bdfqvxyz`~|+_!?@#$%^&*'=()[]{}\"\\;<>";
//    public static final String WHITELIST_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789.,-:/";
    static final String WHITELIST_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZaceghijklmnoprstuw0123456789.,-:/";
    static final String DOWNLOAD_BASE = "http://www.naulinovation.com/tessdata/ocrektp/";
    static final String SAVED_IMAGE_LOC = "/DCIM/OCR/";
    public static final int PERMISSIONS_REQUEST_CAMERA = 8000;
    public static final int PERMISSIONS_REQUEST_STORAGE = 8001;

    public static final int MIN_FRAME_WIDTH = 480; // originally 240
    public static final int MIN_FRAME_HEIGHT = 320; // originally 240
    public static final int MAX_FRAME_WIDTH = 960; // originally 480
    public static final int MAX_FRAME_HEIGHT = 640; // originally 360

    static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()); //format to save bitmap to folder if required

    static final String PROVINCE_PATTERN = "PR[0OoD]V[iI1l]NS[iI1l] [A-Z0134568\\s]+";
    static final String KABUPATEN_PATTERN = "KA[B8]\\w+P[A4]TEN (\\w+)\\s";
    static final String KOTA_PATTERN = "K[O0D]TA\\s*[a-zA-Z0134568\\s]+";
    static final String NIK_PATTERN = "N[IZ:ETt]?K\\s*[-:=1I0\\.]*\\s*(\\w+)";
    static final String NAME_PATTERN = "N[ea][mn]a\\s*[-:=1I0\\.]*\\s*([a-zA-Z0134568\\s\\.]+)";
    static final String KECAMATAN_PATTERN = "Keca[mn]ata[mn]\\s*[-:1I0\\.]*\\s*([a-zA-Z" + Pattern.quote("-") + "0134568\\s]+)";
    static final String BIRTH_PLACE_PATTERN = "[LI]ah[ilI1][rl]\\s*[-:1I0\\.]*\\s*([a-zA-Z0134568\\s\\.]+)";
    static final String BIRTH_DAY_PATTERN = "[LI]ah[ilI1][rl]\\s*[-:1I0\\.]*\\s*(.+?)\\s([0-9-]+)";
    static final String SEX_PATTERN = "Ke[lt]a[mn][iI1]*[nm]\\s*[-:1I0\\.]*\\s*(.*)\\s[G6](.*)";
    static final String ADDRESS_PATTERN = "A[l1I]a[mn]a[trl]\\s[-:1I0\\.]*(.*)";
    static final String BLOOD_TYPE_PATTERN = "G[O0QD][lI1]\\s*[DO0]\\w+h\\s*[-:1I0\\.]*\\s*([0ODAB8])";
    static final String RTRW_PATTERN = "R[TI][/:]*R[WV]\\s*[-:1I0\\.]*\\s*([0-9/\\s]+)";
    static final String DESA_PATTERN = "[D0O]esa\\s*[-:1I0\\.]*\\s*([a-zA-Z0134568\\s]+)";
    static final String RELIGION_PATTERN = "Ag[ao][mn][ao]\\s*[-:1I0\\.]*\\s*([a-zA-Z0134568\\s]+)";
    static final String CITIZENSHIP_PATTERN = "[gs]an[ea]garaa[nm]\\s*[-:1I0\\.]*\\s*([a-zA-Z0134568\\s]+)";
    static final String JOB_PATTERN = "[rn]ker[jI]aan\\s*[-:1I0\\.]*\\s*([a-zA-Z0134568\\s]+)";
    static final String MARITAL_STATUS_PATTERN = "Pe[rt]kaw[iI]*[n|wm]a[nm]\\s*[-:1I0\\.]*\\s*([a-zA-Z0134568\\s]+)"; //Perkaw[i]*[n|wm]an
    static final String EXPIRED_DATE = "H[iI]ngga\\s*[-:1I0\\.]*\\s*([0-9I0-]+)";
    static final String KARYAWAN_SWASTA_PATTERN = "KAR[YV]A[WV]ANSWASTA";
    static final String PELAJAR_PATTERN = "PEL[A4]J[A4]R";
    static final String KRISTEN_PATTERN = "KR[Ii1l]STEN";
    static final String ISLAM_PATTERN = "[:=I\\.][I1l][S5]LAM";
    static final String WNI_PATTERN = "[:=I\\.]WN[Ii1l]";
    static final String KAWIN_PATTERN = "[:=I\\.]KAW[I1l]N";
    static final String BELUM_KAWIN_PATTERN = "BELUMKAW[I1l]N";
    static final String LAKI_2_PATTERN = "LAK[I1l]-*LAK[I1l]";
    static final String RT_RW_VALUE_PATTERN = "\\d\\d\\d/\\d\\d\\d";
    static final String NPWP_PATTERN = "N[Pp][WNw][Pp]\\s*[-:=1I\\.]*\\s*([0-9OBlIDSG\\.\\-\\s]+)\\n";
    static final String TAX_SUBJECT_NAME_PATTERN = "\\-[0-9O][0-9O][0-9O]\\s*\\.\\s*[0-9O][0-90][0-9O]([A-ZzolI0-9\\:\\,\\s\\-\\.\\n]+)\\n";
    static final String REGISTERED_DATE_PATTERN = "Terd[ao]f[tc][ao]r\\s*[-:=1I\\.]*\\s*([0-9\\.\\s\\-OBlIDG]+)";
    static final String ISSUER_PATTERN = "P[eo]n[eo]rb[i1:]t\\s*[-:=1I\\.]*\\s*([a-zA-Z0-9\\s\\-\\.]+)";
}
