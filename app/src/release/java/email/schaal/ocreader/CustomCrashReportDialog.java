package email.schaal.ocreader;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;

import org.acra.dialog.BaseCrashReportDialog;

import email.schaal.ocreader.databinding.CrashDialogBinding;

/**
 * This is the dialog Activity used by ACRA to get authorization from the user
 * to send reports. Requires android:launchMode="singleInstance" in your
 * AndroidManifest to work properly.
 **/
public class CustomCrashReportDialog extends BaseCrashReportDialog implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener {
    private CrashDialogBinding binding;

    @Override
    protected void init(@Nullable Bundle savedInstanceState) {
        binding = CrashDialogBinding.inflate(LayoutInflater.from(this), null, false);

        final AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.report_issue)
                .setView(binding.getRoot())
                .setPositiveButton(android.R.string.ok, this)
                .setNegativeButton(android.R.string.cancel, this)
                .create();

        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.setOnDismissListener(this);

        alertDialog.show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if(which == DialogInterface.BUTTON_POSITIVE) {
            sendCrash(binding.comment.getText().toString(), null);
            dialog.dismiss();
        } else {
            cancelReports();
            dialog.cancel();
        }
        finish();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        finish();
    }
}
