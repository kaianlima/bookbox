package com.example.bookbox.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import com.example.bookbox.R;

public class NotificationHelper {
    
    private static final String CHANNEL_ID = "new_books_channel";
    private static final String CHANNEL_NAME = "Novos Livros";
    private static final String CHANNEL_DESCRIPTION = "Notificações sobre novos livros adicionados";
    private static final int NOTIFICATION_ID = 1001;
    
    private Context context;
    private NotificationManagerCompat notificationManager;
    
    public NotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = NotificationManagerCompat.from(context);
        createNotificationChannel();
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500});
            
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
    
    public void sendNewBookNotification(String title, String author, String genre) {
        android.util.Log.d("NotificationHelper", "Tentando enviar notificação para livro: " + title);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                android.util.Log.w("NotificationHelper", "Permissão de notificação não concedida");
                return;
            }
        }

        if (!notificationManager.areNotificationsEnabled()) {
            android.util.Log.w("NotificationHelper", "Notificações desabilitadas pelo usuário");
            return;
        }
        
        String contentText = String.format("📖 %s\n✍️ Autor: %s\n🏷️ Gênero: %s",
                title, author, genre);
        
        android.util.Log.d("NotificationHelper", "Criando notificação com texto: " + contentText);
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_menu_book_24)
                .setContentTitle("📚 Novo Livro Adicionado!")
                .setContentText(title)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL);
        
        try {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
            android.util.Log.d("NotificationHelper", "Notificação enviada com sucesso!");
        } catch (SecurityException e) {
            android.util.Log.e("NotificationHelper", "Erro de segurança ao enviar notificação", e);
        } catch (Exception e) {
            android.util.Log.e("NotificationHelper", "Erro geral ao enviar notificação", e);
        }
    }

    public static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) 
                == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    public static void requestNotificationPermission(androidx.fragment.app.Fragment fragment, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission(fragment.getContext())) {
                fragment.requestPermissions(
                    new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 
                    requestCode
                );
            }
        }
    }
}