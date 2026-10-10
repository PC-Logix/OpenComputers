package li.cil.oc.util;

import com.google.common.net.InetAddresses;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

public final class InternetFilteringRule {
    private static final Logger LOG = LogManager.getLogger("OpenComputers");
    private static final InternetFilteringRule[] DEFAULT_RULES = {
        new InternetFilteringRule("deny private"),
        new InternetFilteringRule("deny bogon"),
        new InternetFilteringRule("allow all")
    };
    private static final InetAddressRange[] BOGON_MATCHING_RULES = Arrays.stream(new String[] {
        "0.0.0.0/8", "10.0.0.0/8", "100.64.0.0/10", "127.0.0.0/8",
        "169.254.0.0/16", "172.16.0.0/12", "192.0.0.0/24", "192.0.2.0/24",
        "192.168.0.0/16", "198.18.0.0/15", "198.51.100.0/24", "203.0.113.0/24",
        "224.0.0.0/3", "::/128", "::1/128", "::ffff:0:0/96", "::/96",
        "64:ff9b::/96", // NAT64 well-known prefix (RFC 6052)
        "100::/64", "2001:10::/28", "2001:db8::/32", "fc00::/7",
        "fe80::/10", "fec0::/10", "ff00::/8"
    }).map(rule -> {
        String[] parts = rule.split("/", 2);
        return InetAddressRange.parse(parts[0], parts[1]);
    }).toArray(InetAddressRange[]::new);

    private final String ruleString;
    private final BiFunction<InetAddress, String, Optional<Boolean>> validator;
    private boolean invalid;

    public InternetFilteringRule(String ruleString) {
        this.ruleString = ruleString;
        BiFunction<InetAddress, String, Optional<Boolean>> parsed;
        try {
            String[] parts = ruleString.split(" ");
            switch (parts[0]) {
                case "allow", "deny" -> {
                    boolean value = parts[0].equals("allow");
                    List<BiPredicate<InetAddress, String>> predicates = new ArrayList<>();
                    for (int i = 1; i < parts.length; i++) {
                        String[] filter = parts[i].split(":", 2);
                        switch (filter[0]) {
                            case "default" -> {
                                if (!value) {
                                    predicates.add((address, host) -> false);
                                } else {
                                    predicates.add((address, host) -> {
                                        for (InternetFilteringRule rule : DEFAULT_RULES) {
                                            Optional<Boolean> result = rule.apply(address, host);
                                            if (result.isPresent()) return result.get();
                                        }
                                        return false;
                                    });
                                }
                            }
                            case "private" -> predicates.add((address, host) ->
                                address.isAnyLocalAddress() || address.isLoopbackAddress() ||
                                    address.isLinkLocalAddress() || address.isSiteLocalAddress());
                            case "bogon" -> predicates.add((address, host) -> {
                                for (InetAddressRange range : BOGON_MATCHING_RULES) {
                                    if (range.matches(address)) return true;
                                }
                                return false;
                            });
                            case "ipv4" -> predicates.add((address, host) -> address instanceof Inet4Address);
                            case "ipv6" -> predicates.add((address, host) -> address instanceof Inet6Address);
                            case "ipv4-embedded-ipv6" -> predicates.add((address, host) ->
                                address instanceof Inet6Address ipv6 &&
                                    InetAddresses.hasEmbeddedIPv4ClientAddress(ipv6));
                            case "domain" -> {
                                String domain = filter[1];
                                InetAddress[] addresses = InetAddress.getAllByName(domain);
                                predicates.add((address, host) ->
                                    domain.equals(host) || Arrays.stream(addresses).anyMatch(candidate -> candidate.equals(address)));
                            }
                            case "ip" -> {
                                String[] ipParts = filter[1].split("/", 2);
                                if (ipParts.length == 2) {
                                    InetAddressRange range = InetAddressRange.parse(ipParts[0], ipParts[1]);
                                    predicates.add((address, host) -> range.matches(address));
                                } else {
                                    InetAddress ipAddress = InetAddresses.forString(ipParts[0]);
                                    predicates.add((address, host) -> ipAddress.equals(address));
                                }
                            }
                            case "all" -> {
                            }
                            default -> {
                            }
                        }
                    }
                    parsed = (address, host) -> {
                        for (BiPredicate<InetAddress, String> predicate : predicates) {
                            if (!predicate.test(address, host)) return Optional.empty();
                        }
                        return Optional.of(value);
                    };
                }
                case "removeme" -> parsed = (address, host) -> Optional.empty();
                default -> throw new IllegalArgumentException("unknown rule action");
            }
        } catch (Throwable error) {
            LOG.error("Invalid Internet filteringRules rule in configuration: \"" + ruleString + "\".", error);
            invalid = true;
            parsed = (address, host) -> Optional.of(false);
        }
        validator = parsed;
    }

    public String ruleString() {
        return ruleString;
    }

    public boolean invalid() {
        return invalid;
    }

    public Optional<Boolean> apply(InetAddress address, String host) {
        return validator.apply(address, host);
    }

    public static boolean firstMatch(InternetFilteringRule[] rules, InetAddress address, String host,
                                     boolean defaultValue) {
        for (InternetFilteringRule rule : rules) {
            Optional<Boolean> result = rule.apply(address, host);
            if (result.isPresent()) return result.get();
        }
        return defaultValue;
    }
}
