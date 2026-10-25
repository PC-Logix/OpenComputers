package li.cil.oc.server.component;

import com.google.common.net.InetAddresses;
import li.cil.oc.Constants;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.api.prefab.AbstractValue;
import li.cil.oc.util.InternetFilteringRule;
import li.cil.oc.util.ResultWrapper;
import li.cil.oc.util.ThreadPoolFactory;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class InternetCard extends AbstractManagedEnvironment implements DeviceInfo {
    private static final int MAX_HTTP_REDIRECTS = 10;
    protected Context owner;
    protected final Set<Closable> connections = new HashSet<>();

    public InternetCard() {
        setNode(Network.newNode(this, Visibility.Network).withComponent("internet", Visibility.Neighbors).create());
    }

    @Override
    public Component node() { return (Component) super.node(); }

    @Override
    public Map<String, String> getDeviceInfo() {
        return Map.of(DeviceAttribute.Class, DeviceClass.Communication,
            DeviceAttribute.Description, "Internet modem",
            DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
            DeviceAttribute.Product, "SuperLink X-D4NK");
    }

    @Callback(direct = true, doc = "function():boolean -- Returns whether HTTP requests can be made (config setting).")
    public Object[] isHttpEnabled(Context context, Arguments args) { return ResultWrapper.result(Settings.get().httpEnabled()); }

    @Callback(doc = "function(url:string[, postData:string[, headers:table[, method:string[, allowErrorBody:boolean]]]]):userdata -- Starts an HTTP request. If allowErrorBody is true, HTTP error responses will return their body instead of throwing an exception.")
    public synchronized Object[] request(Context context, Arguments args) throws IOException {
        checkOwner(context);
        String address = args.checkString(0);
        if (!Settings.get().internetAccessAllowed()) return ResultWrapper.result(null, "internet access is unavailable");
        if (!Settings.get().httpEnabled()) return ResultWrapper.result(null, "http requests are unavailable");
        if (connections.size() >= Settings.get().maxConnections()) throw new IOException("too many open connections");
        String post = args.isString(1) ? args.checkString(1) : null;
        Map<String, String> headers = new HashMap<>();
        if (args.isTable(2)) {
            for (Object raw : args.checkTable(2).entrySet()) {
                Map.Entry<?, ?> entry = (Map.Entry<?, ?>) raw;
                if (entry.getKey() instanceof String key && entry.getValue() != null) {
                    headers.put(key, entry.getValue().toString());
                }
            }
        }
        if (!Settings.get().httpHeadersEnabled() && !headers.isEmpty()) return ResultWrapper.result(null, "http request headers are unavailable");
        String method = args.isString(3) ? args.checkString(3) : null;
        HTTPRequest request = new HTTPRequest(this, checkAddress(address), post, headers, method, args.optBoolean(4, false));
        connections.add(request);
        return ResultWrapper.result(request);
    }

    @Callback(direct = true, doc = "function():boolean -- Returns whether TCP connections can be made (config setting).")
    public Object[] isTcpEnabled(Context context, Arguments args) { return ResultWrapper.result(Settings.get().tcpEnabled()); }

    @Callback(doc = "function(address:string[, port:number]):userdata -- Opens a new TCP connection. Returns the handle of the connection.")
    public synchronized Object[] connect(Context context, Arguments args) throws Exception {
        checkOwner(context);
        String address = args.checkString(0);
        int port = args.optInteger(1, -1);
        if (!Settings.get().internetAccessAllowed()) return ResultWrapper.result(null, "internet access is unavailable");
        if (!Settings.get().tcpEnabled()) return ResultWrapper.result(null, "tcp connections are unavailable");
        if (connections.size() >= Settings.get().maxConnections()) throw new IOException("too many open connections");
        TCPSocket socket = new TCPSocket(this, checkUri(address, port), port);
        connections.add(socket);
        return ResultWrapper.result(socket);
    }

    private void checkOwner(Context context) {
        if (owner == null || context.node() != owner.node()) throw new IllegalArgumentException("can only be used by the owning computer");
    }

    @Override
    public void onConnect(Node node) {
        super.onConnect(node);
        if (owner == null && node.host() instanceof Context context && node.isNeighborOf(node())) owner = context;
    }

    private void closeConnections() {
        // Each handle removes itself when closed; iterate a snapshot of the registered handles.
        for (Closable connection : new ArrayList<>(connections)) connection.close();
        connections.clear();
    }

    @Override
    public synchronized void onDisconnect(Node node) {
        super.onDisconnect(node);
        if (owner != null && (node == node() || node.host() instanceof Context context && Objects.equals(context, owner))) {
            owner = null;
            closeConnections();
        }
    }

    @Override
    public synchronized void onMessage(Message message) {
        super.onMessage(message);
        Object[] data = message.data();
        if (data != null && data.length == 0 &&
            ("computer.stopped".equals(message.name()) || "computer.started".equals(message.name())) &&
            owner != null && Objects.equals(message.source().address(), owner.node().address())) closeConnections();
    }

    private URI checkUri(String address, int port) throws Exception {
        try {
            URI parsed = new URI(address);
            if (parsed.getHost() != null && (parsed.getPort() > 0 || port > 0)) return parsed;
        }
        catch (Throwable ignored) {}
        URI simple = new URI("oc://" + address);
        if (simple.getHost() != null) {
            if (simple.getPort() > 0) return simple;
            if (port > 0) return new URI(simple + ":" + port);
        }
        throw new IllegalArgumentException("address could not be parsed or no valid port given");
    }

    private URL checkAddress(String address) throws FileNotFoundException {
        URL url;
        try { url = new URL(address); }
        catch (Throwable error) { throw new FileNotFoundException("invalid address"); }
        if (!url.getProtocol().matches("^https?$")) throw new FileNotFoundException("unsupported protocol");
        return url;
    }

    public interface Closable { void close(); }

    private static final class InternetRuntime {
        static final ScheduledExecutorService THREAD_POOL = ThreadPoolFactory.create("Internet",
            Settings.get() == null ? 1 : Settings.get().internetThreads());
        static final TCPNotifier NOTIFIER = new TCPNotifier();
        static { NOTIFIER.start(); }
    }

    private interface ReadableAction { void run() throws IOException; }
    private record PendingSocket(SocketChannel channel, ReadableAction action) {}

    private static final class TCPNotifier extends Thread {
        private Selector selector;
        private final ConcurrentLinkedQueue<PendingSocket> toAccept = new ConcurrentLinkedQueue<>();

        TCPNotifier() {
            try { selector = Selector.open(); }
            catch (IOException error) { throw new UncheckedIOException(error); }
        }

        @Override
        public void run() {
            while (true) {
                try {
                    PendingSocket pending;
                    while ((pending = toAccept.poll()) != null) pending.channel().register(selector, SelectionKey.OP_READ, pending.action());
                    selector.select();
                    Set<SelectionKey> readable = new HashSet<>();
                    for (SelectionKey key : selector.selectedKeys()) {
                        if (key.isReadable()) {
                            ((ReadableAction) key.attachment()).run();
                            readable.add(key);
                        }
                    }
                    if (!readable.isEmpty()) {
                        Selector next = Selector.open();
                        for (SelectionKey key : selector.keys()) {
                            if (!readable.contains(key)) key.channel().register(next, SelectionKey.OP_READ, key.attachment());
                        }
                        selector.close();
                        selector = next;
                    }
                }
                catch (IOException error) { OpenComputers.log().error("Error in TCP selector loop.", error); }
            }
        }

        void add(SocketChannel channel, ReadableAction action) {
            toAccept.offer(new PendingSocket(channel, action));
            selector.wakeup();
        }
    }

    public static class TCPSocket extends AbstractValue implements Closable {
        private InternetCard owner;
        private Future<InetAddress> address;
        private SocketChannel channel;
        private boolean isAddressResolved;
        private final UUID id = UUID.randomUUID();

        public TCPSocket() {}

        public TCPSocket(InternetCard owner, URI uri, int port) throws IOException {
            this.owner = owner;
            channel = SocketChannel.open();
            channel.configureBlocking(false);
            address = InternetRuntime.THREAD_POOL.submit(() -> {
                InetAddress resolved = InetAddress.getByName(uri.getHost());
                checkLists(resolved, uri.getHost());
                channel.connect(new InetSocketAddress(resolved, uri.getPort() != -1 ? uri.getPort() : port));
                return resolved;
            });
        }

        private void setupSelector() {
            if (channel == null) return;
            InternetRuntime.NOTIFIER.add(channel, () -> {
                if (owner != null) owner.node().sendToVisible("computer.signal", "internet_ready", id.toString());
                else channel.close();
            });
        }

        @Callback(doc = "function():boolean -- Ensures a socket is connected. Errors if the connection failed.")
        public Object[] finishConnect(Context context, Arguments args) throws Exception {
            Object[] result;
            synchronized (this) { result = ResultWrapper.result(checkConnected()); }
            setupSelector();
            return result;
        }

        @Callback(doc = "function([n:number]):string -- Tries to read data from the socket stream. Returns the read byte array.")
        public synchronized Object[] read(Context context, Arguments args) throws Exception {
            int count = Math.min(Settings.get().maxReadBuffer(), Math.max(0, args.optInteger(0, Integer.MAX_VALUE)));
            if (!checkConnected()) return ResultWrapper.result(new byte[0]);
            ByteBuffer buffer = ByteBuffer.allocate(count);
            int read = channel.read(buffer);
            if (read == -1) return ResultWrapper.result();
            setupSelector();
            return ResultWrapper.result(Arrays.copyOfRange(buffer.array(), 0, read));
        }

        @Callback(doc = "function(data:string):number -- Tries to write data to the socket stream. Returns the number of bytes written.")
        public synchronized Object[] write(Context context, Arguments args) throws Exception {
            return ResultWrapper.result(checkConnected() ? channel.write(ByteBuffer.wrap(args.checkByteArray(0))) : 0);
        }

        @Callback(direct = true, doc = "function() -- Closes an open socket stream.")
        public synchronized Object[] close(Context context, Arguments args) { close(); return null; }

        @Callback(direct = true, doc = "function():string -- Returns connection ID.")
        public synchronized Object[] id(Context context, Arguments args) { return ResultWrapper.result(id.toString()); }

        @Override
        public void dispose(Context context) { super.dispose(context); close(); }

        @Override
        public void close() {
            if (owner != null) {
                owner.connections.remove(this);
                address.cancel(true);
                try { channel.close(); }
                catch (IOException error) { throw new UncheckedIOException(error); }
                owner = null;
                address = null;
                channel = null;
            }
        }

        private boolean checkConnected() throws Exception {
            if (owner == null) throw new IOException("connection lost");
            try {
                if (isAddressResolved) return channel.finishConnect();
                if (address.isCancelled()) {
                    channel.close();
                    throw new IOException("bad connection descriptor");
                }
                if (address.isDone()) {
                    getResult(address);
                    isAddressResolved = true;
                    return channel.finishConnect();
                }
                return false;
            }
            catch (Exception | Error error) { close(); throw error; }
        }
    }

    private static boolean isNAT64Address(Inet6Address address) {
        byte[] bytes = address.getAddress();
        return bytes[0] == 0 && bytes[1] == 0x64 && bytes[2] == (byte) 0xff && bytes[3] == (byte) 0x9b &&
            bytes[4] == 0 && bytes[5] == 0 && bytes[6] == 0 && bytes[7] == 0 &&
            bytes[8] == 0 && bytes[9] == 0 && bytes[10] == 0 && bytes[11] == 0;
    }

    public static boolean isRequestAllowed(Settings settings, InetAddress address, String host) throws IOException {
        if (!settings.internetAccessAllowed()) return false;
        InternetFilteringRule[] rules = settings.internetFilteringRules();
        if (address instanceof Inet6Address ipv6) {
            if (InetAddresses.hasEmbeddedIPv4ClientAddress(ipv6) &&
                !InternetFilteringRule.firstMatch(rules, InetAddresses.getEmbeddedIPv4ClientAddress(ipv6), host, true)) return false;
            if (isNAT64Address(ipv6)) {
                InetAddress embedded = InetAddress.getByAddress(Arrays.copyOfRange(ipv6.getAddress(), 12, 16));
                if (!InternetFilteringRule.firstMatch(rules, embedded, host, true)) return false;
            }
            return InternetFilteringRule.firstMatch(rules, ipv6, host, false);
        }
        if (address instanceof Inet4Address) return InternetFilteringRule.firstMatch(rules, address, host, false);
        OpenComputers.log().warn("Internet Card blocked unrecognized address type: " + address);
        return false;
    }

    public static void checkLists(InetAddress address, String host) throws IOException {
        if (!isRequestAllowed(Settings.get(), address, host)) throw new FileNotFoundException("address is not allowed");
    }

    public static InputStream responseStream(HttpURLConnection http, boolean allowErrorBody) throws IOException {
        int code = http.getResponseCode();
        if (code >= 200 && code < 300) return http.getInputStream();
        if (allowErrorBody) {
            InputStream error = http.getErrorStream();
            return error != null ? error : new ByteArrayInputStream(new byte[0]);
        }
        return http.getInputStream();
    }

    private static <T> T getResult(Future<T> future) throws Exception {
        try { return future.get(); }
        catch (ExecutionException error) {
            Throwable cause = error.getCause();
            if (cause instanceof Exception exception) throw exception;
            if (cause instanceof Error failure) throw failure;
            throw new RuntimeException(cause);
        }
    }

    public static class HTTPRequest extends AbstractValue implements Closable {
        private InternetCard owner;
        private HttpResponse response;
        private Future<InputStream> stream;
        private final ConcurrentLinkedQueue<Byte> queue = new ConcurrentLinkedQueue<>();
        private Future<?> reader;
        private boolean eof;

        public HTTPRequest() {}

        public HTTPRequest(InternetCard owner, URL url, scala.Option<String> post,
                           scala.collection.immutable.Map<String, String> headers, scala.Option<String> method,
                           boolean allowErrorBody) {
            this(owner, url, post.isDefined() ? post.get() : null, scala.jdk.javaapi.CollectionConverters.asJava(headers),
                method.isDefined() ? method.get() : null, allowErrorBody);
        }

        public HTTPRequest(InternetCard owner, URL url, scala.Option<String> post,
                           scala.collection.immutable.Map<String, String> headers, scala.Option<String> method) {
            this(owner, url, post, headers, method, false);
        }


        public HTTPRequest(InternetCard owner, URL url, String post, Map<String, String> headers, String method, boolean allowErrorBody) {
            this.owner = owner;
            stream = InternetRuntime.THREAD_POOL.submit(() -> sendRequest(url, post, headers, method, allowErrorBody));
        }

        @Callback(doc = "function():boolean -- Ensures a response is available. Errors if the connection failed.")
        public synchronized Object[] finishConnect(Context context, Arguments args) throws Exception {
            return ResultWrapper.result(checkResponse());
        }

        @Callback(direct = true, doc = "function():number, string, table -- Get response code, message and headers.")
        public synchronized Object[] response(Context context, Arguments args) {
            return response == null ? ResultWrapper.result() : ResultWrapper.result(response.code(), response.message(), response.headers());
        }

        @Callback(doc = "function([n:number]):string -- Tries to read data from the response. Returns the read byte array.")
        public synchronized Object[] read(Context context, Arguments args) throws Exception {
            int count = Math.min(Settings.get().maxReadBuffer(), Math.max(0, args.optInteger(0, Integer.MAX_VALUE)));
            if (!checkResponse()) return ResultWrapper.result(new byte[0]);
            if (eof && queue.isEmpty()) return ResultWrapper.result();
            ByteBuffer buffer = ByteBuffer.allocate(count);
            int read = 0;
            while (!queue.isEmpty() && read < count) { buffer.put(queue.poll()); read++; }
            if (read == 0) readMore();
            return ResultWrapper.result(Arrays.copyOfRange(buffer.array(), 0, read));
        }

        @Callback(direct = true, doc = "function() -- Closes an open socket stream.")
        public synchronized Object[] close(Context context, Arguments args) { close(); return null; }

        @Override
        public void dispose(Context context) { super.dispose(context); close(); }

        @Override
        public void close() {
            if (owner != null) {
                owner.connections.remove(this);
                stream.cancel(true);
                if (reader != null) reader.cancel(true);
                owner = null;
                stream = null;
                reader = null;
            }
        }

        private synchronized boolean checkResponse() throws Exception {
            if (owner == null) throw new IOException("connection lost");
            if (!stream.isDone()) return false;
            if (reader == null) { getResult(stream); readMore(); }
            return true;
        }

        private void readMore() {
            if ((reader == null || reader.isCancelled() || reader.isDone()) && !eof) {
                reader = InternetRuntime.THREAD_POOL.submit(() -> {
                    try {
                        byte[] buffer = new byte[Settings.get().maxReadBuffer()];
                        int count = stream.get().read(buffer);
                        if (count < 0) eof = true;
                        for (int i = 0; i < count; i++) queue.add(buffer[i]);
                    }
                    catch (Exception error) { throw new RuntimeException(error); }
                });
            }
        }

        private InputStream sendRequest(URL url, String post, Map<String, String> headers, String method, boolean allowErrorBody) throws IOException {
            try {
                URL currentUrl = url;
                String currentMethod = method != null ? method : post != null ? "POST" : "GET";
                String currentPost = post;
                Map<String, String> currentHeaders = new HashMap<>(headers);
                for (int redirects = 0; ; redirects++) {
                    checkLists(InetAddress.getByName(currentUrl.getHost()), currentUrl.getHost());
                    var connection = currentUrl.openConnection(ServerLifecycleHooks.getCurrentServer().getProxy());
                    if (!(connection instanceof HttpURLConnection http)) throw new IOException("unexpected connection type");
                    try {
                        http.setInstanceFollowRedirects(false);
                        http.setConnectTimeout(Settings.get().httpTimeout());
                        http.setReadTimeout(Settings.get().httpTimeout());
                        http.setDoInput(true);
                        http.setDoOutput(currentPost != null);
                        http.setRequestMethod(currentMethod);
                        http.setRequestProperty("User-Agent", Settings.get().httpUserAgent().replace("$version", OpenComputers.Version()));
                        currentHeaders.forEach(http::setRequestProperty);
                        if (currentPost != null) {
                            try (BufferedWriter output = new BufferedWriter(new OutputStreamWriter(http.getOutputStream()))) { output.write(currentPost); }
                        }

                        int code = http.getResponseCode();
                        String location = http.getHeaderField("Location");
                        if (!isRedirect(code) || location == null || location.isBlank()) {
                            synchronized (this) { response = new HttpResponse(code, http.getResponseMessage(), http.getHeaderFields()); }
                            return responseStream(http, allowErrorBody);
                        }
                        if (redirects >= MAX_HTTP_REDIRECTS) throw new IOException("too many redirects");

                        URL nextUrl = new URL(currentUrl, location);
                        if (!currentUrl.getProtocol().equalsIgnoreCase(nextUrl.getProtocol()) ||
                            !currentUrl.getHost().equalsIgnoreCase(nextUrl.getHost()) || effectivePort(currentUrl) != effectivePort(nextUrl)) {
                            currentHeaders.keySet().removeIf(name -> name.equalsIgnoreCase("Authorization") ||
                                name.equalsIgnoreCase("Cookie") || name.equalsIgnoreCase("Cookie2"));
                        }
                        if (code == HttpURLConnection.HTTP_SEE_OTHER && !currentMethod.equalsIgnoreCase("HEAD") ||
                            (code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_MOVED_TEMP) &&
                                !currentMethod.equalsIgnoreCase("GET") && !currentMethod.equalsIgnoreCase("HEAD")) {
                            currentMethod = "GET";
                            currentPost = null;
                        }
                        currentUrl = nextUrl;
                    }
                    catch (Throwable error) { http.disconnect(); throw error; }
                    http.disconnect();
                }
            }
            catch (UnknownHostException error) { throw new IOException("unknown host: " + (error.getMessage() != null ? error.getMessage() : error.toString())); }
            catch (Throwable error) { throw new IOException(error.getMessage() != null ? error.getMessage() : error.toString()); }
        }

        private static boolean isRedirect(int code) {
            return code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_MOVED_TEMP ||
                code == HttpURLConnection.HTTP_SEE_OTHER || code == 300 || code == 307 || code == 308;
        }

        private static int effectivePort(URL url) {
            return url.getPort() != -1 ? url.getPort() : url.getDefaultPort();
        }

        private record HttpResponse(int code, String message, Object headers) {}
    }
}
