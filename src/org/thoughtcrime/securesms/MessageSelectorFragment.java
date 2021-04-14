package org.thoughtcrime.securesms;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.util.Linkify;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.Fragment;

import com.b44t.messenger.DcMsg;

import org.thoughtcrime.securesms.connect.ApplicationDcContext;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.util.SaveAttachmentTask;

import java.util.Set;

public class MessageSelectorFragment extends Fragment
{
  protected ActionMode actionMode;
  protected ApplicationDcContext dcContext;

  protected void handleDisplayDetails(DcMsg dcMsg) {
    String infoStr = dcContext.getMsgInfo(dcMsg.getId());
    AlertDialog d = new AlertDialog.Builder(getActivity())
            .setMessage(infoStr)
            .setPositiveButton(android.R.string.ok, null)
            .create();
    d.show();
    try {
      //noinspection ConstantConditions
      Linkify.addLinks((TextView) d.findViewById(android.R.id.message), Linkify.WEB_URLS);
    } catch(NullPointerException e) {
      e.printStackTrace();
    }
  }

  protected void handleDeleteMessages(final Set<DcMsg> messageRecords) {
    int messagesCount = messageRecords.size();

    new AlertDialog.Builder(getActivity())
            .setMessage(getActivity().getResources().getQuantityString(R.plurals.ask_delete_messages, messagesCount, messagesCount))
            .setCancelable(true)
            .setPositiveButton(R.string.delete, (dialog, which) -> {
                int[] ids = DcMsg.msgSetToIds(messageRecords);
                dcContext.deleteMsgs(ids);
                actionMode.finish();
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
  }

  protected void handleSaveAttachment(final DcMsg message) {
    SaveAttachmentTask.showWarningDialog(getContext(), (dialogInterface, i) -> {
        Permissions.with(getActivity())
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                .ifNecessary()
                .withPermanentDenialDialog(getString(R.string.perm_explain_access_to_storage_denied))
                .onAllGranted(() -> {
                    SaveAttachmentTask saveTask = new SaveAttachmentTask(getContext());
                    SaveAttachmentTask.Attachment attachment = new SaveAttachmentTask.Attachment(
                            Uri.fromFile(message.getFileAsFile()), message.getFilemime(), message.getDateReceived(), message.getFilename());
                    saveTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, attachment);
                    actionMode.finish();
                })
                .execute();
    });
  }

  protected void handleShowInChat(final DcMsg dcMsg) {
    Intent intent = new Intent(getContext(), ConversationActivity.class);
    intent.putExtra(ConversationActivity.CHAT_ID_EXTRA, dcMsg.getChatId());
    intent.putExtra(ConversationActivity.STARTING_POSITION_EXTRA, DcMsg.getMessagePosition(dcMsg, dcContext));
    startActivity(intent);
  }
}
