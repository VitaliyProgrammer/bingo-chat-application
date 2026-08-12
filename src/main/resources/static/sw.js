self.addEventListener('push', (event) => {
    let data = {};
    try {
        data = event.data ? event.data.json() : {};
    } catch (e) {
        data = {title: 'Bingo Chat', body: event.data ? event.data.text() : ''};
    }

    const title = data.title || 'Bingo Chat';
    const options = {
        body: data.body || '',
        icon: '/icon.png',
        badge: '/icon.png',
        tag: data.chatId ? 'chat-' + data.chatId : undefined,
        renotify: !!data.chatId,
        data: {chatId: data.chatId}
    };

    event.waitUntil(self.registration.showNotification(title, options));
});

self.addEventListener('notificationclick', (event) => {
    event.notification.close();

    const chatId = event.notification.data && event.notification.data.chatId;
    const targetUrl = '/stomp-test.html' + (chatId ? '#chat-' + chatId : '');

    event.waitUntil(
        clients.matchAll({type: 'window', includeUncontrolled: true}).then((windowClients) => {
            for (const client of windowClients) {
                if ('focus' in client) {
                    return client.focus();
                }
            }
            if (clients.openWindow) {
                return clients.openWindow(targetUrl);
            }
        })
    );
});
