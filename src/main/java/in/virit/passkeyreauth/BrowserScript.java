package in.virit.passkeyreauth;

/**
 * The browser side of the re-authentication: asks the authenticator for an
 * assertion and returns it in the standard JSON format (as defined for
 * {@code PublicKeyCredential.toJSON()}). Base64URL conversions are done by hand,
 * as the newer {@code parseRequestOptionsFromJSON}/{@code toJSON} helpers are
 * not yet available in all browsers in use.
 * <p>
 * Parameters: $0 challenge, $1 rpId, $2 timeout (ms), $3 comma separated
 * credential ids (Base64URL) of the current user.
 */
final class BrowserScript {

    static final String GET_ASSERTION = """
            const fromB64u = s => Uint8Array.from(
                atob(s.replace(/-/g, '+').replace(/_/g, '/').padEnd(Math.ceil(s.length / 4) * 4, '=')),
                c => c.charCodeAt(0));
            const toB64u = buffer => btoa(String.fromCharCode(...new Uint8Array(buffer)))
                .replace(/\\+/g, '-').replace(/\\//g, '_').replace(/=+$/, '');
            return navigator.credentials.get({
                publicKey: {
                    challenge: fromB64u($0),
                    rpId: $1,
                    timeout: $2,
                    userVerification: 'required',
                    allowCredentials: $3.split(',').map(id => ({type: 'public-key', id: fromB64u(id)}))
                }
            }).then(credential => JSON.stringify({
                id: credential.id,
                rawId: toB64u(credential.rawId),
                type: credential.type,
                authenticatorAttachment: credential.authenticatorAttachment,
                clientExtensionResults: credential.getClientExtensionResults(),
                response: {
                    clientDataJSON: toB64u(credential.response.clientDataJSON),
                    authenticatorData: toB64u(credential.response.authenticatorData),
                    signature: toB64u(credential.response.signature),
                    userHandle: credential.response.userHandle
                        ? toB64u(credential.response.userHandle) : null
                }
            }));
            """;

    private BrowserScript() {
    }
}
