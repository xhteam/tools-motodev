/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eclipse.andmore.android.certmanager.packaging.sign;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.eclipse.andmore.android.certmanager.CertificateManagerActivator;
import org.eclipse.andmore.android.certmanager.exception.KeyStoreManagerException;
import org.eclipse.andmore.android.certmanager.ui.model.IKeyStoreEntry;
import org.eclipse.andmore.android.common.log.AndmoreLogger;

/**
 * This class implements the signature block file from jar mechanism used in
 * packaging
 */
public class SignatureBlockFile {

	/**
	 * the signature file
	 */
	private SignatureFile signatureFile;

	/**
	 * A certificate from keystore
	 */
	private IKeyStoreEntry keystoreEntry;

	/**
	 * The password of the certificate.
	 */
	private String keyEntryPassword;

	/**
	 * Default Constructor
	 * 
	 * @param signatureFile
	 *            the signature file
	 * @param alias
	 *            the certificate alias
	 */
	public SignatureBlockFile(SignatureFile signatureFile, IKeyStoreEntry keystoreEntry, String keyEntryPassword) {
		this.keyEntryPassword = keyEntryPassword;
		this.keystoreEntry = keystoreEntry;
		this.signatureFile = signatureFile;
	}

	/**
	 * To string method override
	 * 
	 * @return the signature block file name with relative path from root.
	 *         Frequently META-INF/alias.RSA or .DSA
	 */
	@Override
	public String toString() {
		String result = new String();
		try {
			result = new StringBuilder(CertificateManagerActivator.METAFILES_DIR)
					.append(CertificateManagerActivator.JAR_SEPARATOR).append(ISignConstants.SIGNATURE_FILE_NAME)
					.append(".").append(getBlockAlgorithm()).toString();
		} catch (UnrecoverableKeyException e) {
			AndmoreLogger.error("Could not generate signature block file name.");
		} catch (KeyStoreException e) {
			AndmoreLogger.error("Could not generate signature block file name.");
		} catch (NoSuchAlgorithmException e) {
			AndmoreLogger.error("Could not generate signature block file name.");
		} catch (KeyStoreManagerException e) {
			AndmoreLogger.error("Could not generate signature block file name.");
		}

		return result;
	}

	/**
	 * Gets the block file algorithm
	 * 
	 * @return the signature block file algorithm to be used
	 * @throws KeyStoreManagerException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 * @throws UnrecoverableKeyException
	 * @throws
	 */
	private String getBlockAlgorithm() throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException,
			KeyStoreManagerException {
		return keystoreEntry.getKey(this.keyEntryPassword).getAlgorithm();
	}

	/**
	 * Writes this file to an output stream
	 * 
	 * @param outputStream
	 *            the output stream to write the file
	 * @throws IOException
	 *             if an I/O error occurs during the signing process
	 * @throws SignException
	 *             if a processing error occurs during the signing process
	 * @throws KeyStoreManagerException
	 * @throws KeyStoreException
	 * @throws UnrecoverableKeyException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeyException 
	 * @throws CertificateEncodingException 
	 * @throws OperatorCreationException 
	 * @throws CMSException 
	 */
	public void write(OutputStream outputStream) throws IOException, SignException, UnrecoverableKeyException,
			KeyStoreException, KeyStoreManagerException, NoSuchAlgorithmException, InvalidKeyException,
			CertificateEncodingException, OperatorCreationException, CMSException {
		// get certificate from entry
		X509Certificate[] certChain = { keystoreEntry.getX509Certificate() };
		if (certChain.length > 0) {
			X509Certificate publicKey = certChain[0];
			PrivateKey privateKey = keystoreEntry.getPrivateKey(keyEntryPassword);
			String blockalgorithm = getBlockAlgorithm();
			if (!blockalgorithm.equalsIgnoreCase(ISignConstants.DSA)
					&& !blockalgorithm.equalsIgnoreCase(ISignConstants.RSA)) {
				AndmoreLogger.error(SignatureBlockFile.class,
						"Signing block algorithm not supported. Key algorithm must be DSA or RSA");
				throw new SignException("Signing block algorithm not supported");
			}

			String signatureAlgorithm = ISignConstants.SHA1 + ISignConstants.ALGORITHM_CONNECTOR + blockalgorithm;

			Security.addProvider(new BouncyCastleProvider());

			ArrayList<X509Certificate> certList = new ArrayList<X509Certificate>();
			certList.add(publicKey);
			JcaCertStore certs = new JcaCertStore(certList);

			ContentSigner signer = new JcaContentSignerBuilder(signatureAlgorithm).build(privateKey);

			CMSSignedDataGenerator generator = new CMSSignedDataGenerator();
			generator.addSignerInfoGenerator(
					new JcaSignerInfoGeneratorBuilder(
							new JcaDigestCalculatorProviderBuilder()
							.build())
					.setDirectSignature(true)
					.build(signer, publicKey));
			generator.addCertificates(certs);

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			signatureFile.write(baos);

			CMSTypedData cmsdata = new CMSProcessableByteArray(baos.toByteArray());
			CMSSignedData signeddata = generator.generate(cmsdata, false);

			ASN1InputStream asn1 = new ASN1InputStream(signeddata.getEncoded());
			DEROutputStream dos = new DEROutputStream(outputStream);
			dos.writeObject(asn1.readObject());
			dos.flush();
			dos.close();
			asn1.close();
		}
		AndmoreLogger.info(SignatureBlockFile.class, "Created signature block file");
	}
}
