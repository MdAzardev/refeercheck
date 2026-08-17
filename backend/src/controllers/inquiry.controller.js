const path = require('path');
const fs = require('fs');
const ExcelJS = require('exceljs');
const PDFDocument = require('pdfkit');
const SVGtoPDF = require('svg-to-pdfkit');
const updateSvgText = require('../utils/svgwriter');
const { transporter } = require('../services/email.service');

const INPUT_DIR = process.env.INPUT_DIR ? path.resolve(process.env.INPUT_DIR) : path.join(__dirname, '../../input');
const OUTPUT_DIR = process.env.OUTPUT_DIR ? path.resolve(process.env.OUTPUT_DIR) : path.join(__dirname, '../../output');

// Ensure output directory exists
if (!fs.existsSync(OUTPUT_DIR)) {
  fs.mkdirSync(OUTPUT_DIR, { recursive: true });
}

// In-memory verification code store
const codes = {};

// 📩 HTML Verification Email Template
const emailTemplate = (code) => `
  <div style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f5f7fa; padding: 40px;">
    <div style="max-width: 600px; margin: 0 auto; background: white; border-radius: 10px; padding: 30px; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">
      <h2 style="color: #2c3e50;">🔐 Email Verification Code</h2>
      <p style="font-size: 16px; color: #34495e;">Hello,</p>
      <p style="font-size: 16px; color: #34495e;">Please use the following code to verify your email address:</p>
      <div style="margin: 20px 0; text-align: center;">
        <span style="font-size: 36px; font-weight: bold; letter-spacing: 6px; color: #3498db;">
          ${code}
        </span>
      </div>
      <p style="font-size: 14px; color: #7f8c8d;">This code will expire in 10 minutes. If you did not request this, you can ignore this email.</p>
      <hr style="margin-top: 30px; border: none; border-top: 1px solid #ecf0f1;" />
      <p style="font-size: 12px; color: #95a5a6; text-align: center;">
        &copy; ${new Date().getFullYear()} MR RAWTHER ELTECH. All rights reserved.
      </p>
    </div>
  </div>
`;

// 📩 Send Verification Code
const sendCode = async (req, res) => {
  const { email } = req.body;
  if (!email) return res.status(400).json({ error: 'Email is required' });

  const code = Math.floor(100000 + Math.random() * 900000).toString();
  codes[email] = { code, createdAt: Date.now() };

  try {
    await transporter.sendMail({
      from: `"Support Team" <${process.env.EMAIL_USER}>`,
      to: email,
      subject: 'Your Verification Code',
      html: emailTemplate(code),
    });

    res.json({ message: 'Verification code sent to email successfully' });
  } catch (err) {
    console.error('Email send error:', err);
    res.status(500).json({ error: 'Failed to send email' });
  }
};

// ✅ Verify Code
const verifyCode = (req, res) => {
  const { email, code } = req.body;
  const record = codes[email];

  if (!record) return res.status(400).json({ error: 'Code not found for this email' });

  const expired = Date.now() - record.createdAt > 10 * 60 * 1000;
  if (expired) {
    delete codes[email];
    return res.status(400).json({ error: 'Code expired' });
  }

  if (record.code === code) {
    delete codes[email];
    return res.json({ message: 'Email verified successfully' });
  } else {
    return res.status(400).json({ error: 'Invalid code' });
  }
};

// 📦 Product Information Inquiry
const productInfo = async (req, res) => {
  const { customerName, email, phone, country, productDetails } = req.body;

  if (!customerName || !email || !phone || !productDetails || !Array.isArray(productDetails)) {
    return res.status(400).json({ error: 'Customer information and product details array are required' });
  }

  try {
    const workbook = new ExcelJS.Workbook();

    // Customer Information Sheet
    const customerSheet = workbook.addWorksheet('Customer Information');
    customerSheet.columns = [
      { header: 'Field', key: 'field', width: 20 },
      { header: 'Value', key: 'value', width: 30 }
    ];

    customerSheet.getRow(1).font = { bold: true };
    customerSheet.getRow(1).fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'CCCCCC' } };

    const totalQuantity = productDetails.reduce((sum, product) => sum + (parseInt(product.quantity) || 1), 0);

    customerSheet.addRows([
      ['Customer Name', customerName],
      ['Email', email],
      ['Phone', phone],
      ['Country', country || 'Not specified'],
      ['Inquiry Date', new Date().toLocaleString()],
      ['Total Products', productDetails.length],
      ['Total Quantity', totalQuantity]
    ]);

    // Product Details Sheet
    const productSheet = workbook.addWorksheet('Product Details');
    productSheet.columns = [
      { header: 'ID', key: 'id', width: 8 },
      { header: 'Product Name', key: 'productName', width: 25 },
      { header: 'Description', key: 'description', width: 30 },
      { header: 'Category', key: 'category', width: 15 },
      { header: 'Sub-category', key: 'subCategory', width: 15 },
      { header: 'Type', key: 'type', width: 15 },
      { header: 'SKU', key: 'sku', width: 15 },
      { header: 'Size', key: 'size', width: 12 },
      { header: 'Colour', key: 'colour', width: 12 },
      { header: 'Quantity', key: 'quantity', width: 10 },
      { header: 'Availability', key: 'availability', width: 15 },
      { header: 'Product Link', key: 'productLink', width: 40 },
      { header: 'Image URL', key: 'imgUrl', width: 40 }
    ];

    productSheet.getRow(1).font = { bold: true };
    productSheet.getRow(1).fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'CCCCCC' } };

    const productRows = productDetails.map((product, index) => [
      index + 1,
      product.productName || '',
      product.desc || product.description || '',
      product.category || '',
      product.subCategory || '',
      product.type || '',
      product.sku || '',
      product.size || '',
      product.colour || product.color || '',
      product.quantity || 1,
      product.availability || '',
      product.productLink || product.link || '',
      product.imgUrl || product.imageUrl || ''
    ]);

    productSheet.addRows(productRows);

    const timestamp = Date.now();
    const filename = `inquiry_${customerName.replace(/\s+/g, '_')}_${timestamp}.xlsx`;

    const adminEmailHtml = `
      <div style="font-family:Arial;max-width:600px;margin:0 auto;padding:20px">
        <h2 style="color:#dc3545">New Product Inquiry</h2>
        <div style="background:#f5f5f5;padding:15px;border-radius:5px">
          <h3>Customer Details</h3>
          <p><b>Name:</b> ${customerName}</p>
          <p><b>Email:</b> ${email}</p>
          <p><b>Phone:</b> ${phone}</p>
          ${country ? `<p><b>Country:</b> ${country}</p>` : ''}
          <p><b>Products:</b> ${productDetails.length} | <b>Quantity:</b> ${totalQuantity}</p>
          <p><b>Date:</b> ${new Date().toLocaleString()}</p>
        </div>
        <p style="background:#fff3cd;padding:10px;border-radius:5px">📎 Complete details in Excel attachment</p>
      </div>
    `;

    const userAckHtml = `
      <div style="font-family:Arial;max-width:600px;margin:0 auto;padding:20px">
        <div style="text-align:center;border-bottom:2px solid #dc3545;padding-bottom:15px;margin-bottom:20px">
          <h1 style="color:#dc3545;margin:0">MR RAWTHER ELTECH</h1>
          <p style="color:#666;margin:5px 0 0 0">Professional Marine Components</p>
        </div>
        <div style="text-align:center;margin-bottom:20px">
          <h2 style="color:#28a745">Thank You!</h2>
          <p>We've received your inquiry for ${productDetails.length} products.</p>
        </div>
        <div style="background:#f5f5f5;padding:15px;border-radius:5px">
          <p><b>Name:</b> ${customerName}</p>
          <p><b>Email:</b> ${email}</p>
          <p><b>Phone:</b> ${phone}</p>
          <p><b>Products:</b> ${productDetails.length}</p>
        </div>
        <div style="background:#e3f2fd;padding:15px;margin:20px 0;border-radius:5px">
          <p>📞 We'll contact you within 24 hours</p>
          <p>📄 You'll receive pricing and availability</p>
        </div>
        <div style="text-align:center;background:#f5f5f5;padding:15px;border-radius:5px">
          <p><b>Email:</b> Info@mrrawthereltech.com | <b>Phone:</b> +91 94453 74913</p>
        </div>
      </div>
    `;

    const excelBuffer = await workbook.xlsx.writeBuffer();

    await Promise.all([
      transporter.sendMail({
        from: `"Product Inquiry" <${process.env.EMAIL_USER}>`,
        to: process.env.ADMIN_EMAIL,
        subject: `New Inquiry: ${customerName} - ${productDetails.length} Products`,
        html: adminEmailHtml,
        attachments: [{
          filename: filename,
          content: excelBuffer,
          contentType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
        }]
      }),
      transporter.sendMail({
        from: `"MR RAWTHER ELTECH" <${process.env.EMAIL_USER}>`,
        to: email,
        subject: `Thank you for your inquiry - MR RAWTHER ELTECH`,
        html: userAckHtml
      })
    ]);

    res.json({ 
      success: true,
      message: `Inquiry processed successfully`,
      products: productDetails.length,
      quantity: totalQuantity,
      timestamp: timestamp
    });

  } catch (err) {
    console.error('Processing error:', err);
    res.status(500).json({ error: 'Failed to process inquiry' });
  }
};

// ⚙️ Motor Specification Inquiry with PDF & Excel Generation
const motorInquiry = async (req, res) => {
  console.log('🔍 Motor inquiry received');
  const { customer, motorDetails, dimensions } = req.body;
  const { name: customerName, email, phone, company } = customer || {};

  if (!customerName) return res.status(400).json({ error: 'Customer name is required' });
  if (!email) return res.status(400).json({ error: 'Email is required' });
  if (!phone) return res.status(400).json({ error: 'Phone is required' });
  if (!company) return res.status(400).json({ error: 'Company is required' });
  if (!motorDetails || typeof motorDetails !== 'object') {
    return res.status(400).json({ error: 'Motor details are required and must be an object' });
  }

  try {
    const workbook = new ExcelJS.Workbook();

    // Customer Information
    const customerSheet = workbook.addWorksheet('Customer Information');
    customerSheet.columns = [
      { header: 'Field', key: 'field', width: 25 },
      { header: 'Value', key: 'value', width: 40 }
    ];
    customerSheet.getRow(1).font = { bold: true, color: { argb: 'FFFFFF' } };
    customerSheet.getRow(1).fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: '2196F3' } };

    customerSheet.addRow({ field: 'Customer Name', value: customerName });
    customerSheet.addRow({ field: 'Email', value: email });
    customerSheet.addRow({ field: 'Phone', value: phone });
    customerSheet.addRow({ field: 'Company', value: company });
    customerSheet.addRow({ field: 'Inquiry Date', value: new Date().toLocaleString() });

    // Motor Specifications
    const motorSheet = workbook.addWorksheet('Motor Specifications');
    motorSheet.columns = [
      { header: 'Specification', key: 'spec', width: 30 },
      { header: 'Value', key: 'value', width: 40 }
    ];
    motorSheet.getRow(1).font = { bold: true, color: { argb: 'FFFFFF' } };
    motorSheet.getRow(1).fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'DC3545' } };

    const motorSpecs = [
      { spec: 'Manufacturer', value: motorDetails.manufacturer || 'N/A' },
      { spec: 'Service Type', value: motorDetails.serviceType || 'N/A' },
      { spec: 'Voltage', value: motorDetails.voltage || 'N/A' },
      { spec: 'Frequency', value: motorDetails.frequency || 'N/A' },
      { spec: 'Power', value: motorDetails.power || 'N/A' },
      { spec: 'Amperage', value: motorDetails.amperage || 'N/A' },
      { spec: 'Phases', value: motorDetails.phases || 'N/A' },
      { spec: 'Rating', value: motorDetails.rating || 'N/A' },
      { spec: 'Frame Size', value: motorDetails.frameSize || 'N/A' },
      { spec: 'Serial Number', value: motorDetails.serialNumber || 'N/A' },
      { spec: 'Foot Mounted', value: motorDetails.footMounted || 'N/A' },
      { spec: 'Mounting', value: motorDetails.mounting || 'N/A' },
      { spec: 'Connection', value: motorDetails.connection || 'N/A' },
      { spec: 'RPM', value: motorDetails.rpm || 'N/A' },
      { spec: 'Insulation Class', value: motorDetails.insulationClass || 'N/A' },
      { spec: 'Terminal Box', value: motorDetails.terminalBox || 'N/A' },
      { spec: 'Enclosure Type', value: motorDetails.enclosureType || 'N/A' },
      { spec: 'Weight', value: motorDetails.weight || 'N/A' },
      { spec: 'Bearing Type', value: motorDetails.bearingType || 'N/A' },
      { spec: 'Cable Inlet', value: motorDetails.cableInlet || 'N/A' },
      { spec: 'Electrical Protection', value: motorDetails.electricalProtection || 'N/A' },
      { spec: 'Anti-condensation Heater', value: motorDetails.anticondensationHeater || 'N/A' }
    ];

    motorSpecs.forEach(spec => motorSheet.addRow(spec));

    // Dimensions
    let totalDimensions = 0;
    if (dimensions) {
      const dimensionsSheet = workbook.addWorksheet('Dimensions & Measurements');
      dimensionsSheet.columns = [
        { header: 'Diagram', key: 'diagram', width: 15 },
        { header: 'Dimension', key: 'dimension', width: 30 },
        { header: 'Value', key: 'value', width: 15 },
        { header: 'Unit', key: 'unit', width: 10 },
        { header: 'Notes', key: 'notes', width: 30 }
      ];
      dimensionsSheet.getRow(1).font = { bold: true, color: { argb: 'FFFFFF' } };
      dimensionsSheet.getRow(1).fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: '17A2B8' } };

      Object.entries(dimensions).forEach(([diagramKey, diagramDimensions]) => {
        if (diagramDimensions && typeof diagramDimensions === 'object') {
          Object.entries(diagramDimensions).forEach(([dimensionKey, value]) => {
            if (value && value.toString().trim() !== '') {
              dimensionsSheet.addRow({ 
                diagram: diagramKey,
                dimension: dimensionKey, 
                value: value,
                unit: 'mm',
                notes: `Motor diagram ${diagramKey} measurements`
              });
              totalDimensions++;
            }
          });
        }
      });
    }

    const excelBuffer = await workbook.xlsx.writeBuffer();
    const timestamp = new Date().toISOString().slice(0, 19).replace(/[T:]/g, '-');
    const excelFilename = `Motor_Inquiry_${customerName.replace(/\s+/g, '_')}_${timestamp}.xlsx`;

    let pdfBuffer = null;
    let pdfFilename = null;
    let totalSVGs = 0;

    if (dimensions && Object.keys(dimensions).length > 0 && fs.existsSync(INPUT_DIR)) {
      try {
        const svgFiles = fs.readdirSync(INPUT_DIR).filter(f => f.endsWith('.svg'));
        const updatedSvgPaths = [];
        totalSVGs = svgFiles.length;

        for (const file of svgFiles) {
          const diagramName = path.basename(file, '.svg');
          const dimensionUpdates = dimensions[diagramName];
          if (!dimensionUpdates) continue;

          const svgPath = path.join(INPUT_DIR, file);
          const svgText = fs.readFileSync(svgPath, 'utf-8');
          const updatedSvg = updateSvgText(svgText, dimensionUpdates);
          const outPath = path.join(OUTPUT_DIR, `${diagramName}_updated.svg`);
          fs.writeFileSync(outPath, updatedSvg);
          updatedSvgPaths.push(outPath);
        }

        if (updatedSvgPaths.length > 0) {
          const doc = new PDFDocument({ size: 'A4', margin: 20, bufferPages: true });
          const buffers = [];
          
          doc.on('data', buffers.push.bind(buffers));

          const pageWidth = 595;
          const pageHeight = 842;
          const margin = 20;

          function addHeader() {
            doc.fontSize(22).fillColor('#dc3545').font('Helvetica-Bold');
            doc.text('MR RAWTHER ELTECH', margin, 30, { width: pageWidth - 2 * margin, align: 'center' });
            doc.fontSize(14).fillColor('#666666').font('Helvetica');
            doc.text('Motor Specifications & Diagrams', margin, 55, { width: pageWidth - 2 * margin, align: 'center' });
            doc.fontSize(10).fillColor('#999999');
            doc.text(`Generated on ${new Date().toLocaleDateString()}`, margin, 75, { width: pageWidth - 2 * margin, align: 'center' });
            doc.strokeColor('#dc3545').lineWidth(2).moveTo(margin, 95).lineTo(pageWidth - margin, 95).stroke();
          }

          addHeader();
          let yPos = 120;
          doc.fontSize(16).fillColor('#dc3545').font('Helvetica-Bold');
          doc.text('Customer Information', margin, yPos);
          doc.rect(margin, yPos + 15, pageWidth - 2 * margin, 140).fillAndStroke('#f8f9fa', '#dee2e6');
          
          yPos += 30;
          doc.fontSize(12).fillColor('#000000').font('Helvetica');
          doc.text(`Name: ${customerName}`, margin + 15, yPos);
          yPos += 22;
          doc.text(`Company: ${company}`, margin + 15, yPos);
          yPos += 22;
          doc.text(`Email: ${email}`, margin + 15, yPos);
          yPos += 22;
          doc.text(`Phone: ${phone}`, margin + 15, yPos);
          yPos += 22;
          
          const manufacturer = motorSpecs.find(s => s.spec === 'Manufacturer')?.value || 'N/A';
          const power = motorSpecs.find(s => s.spec === 'Power')?.value || 'N/A';
          doc.text(`Motor: ${manufacturer} - ${power}`, margin + 15, yPos);

          updatedSvgPaths.forEach((svgPath) => {
            doc.addPage();
            addHeader();
            const svgName = path.basename(svgPath, '_updated.svg');
            const svgContent = fs.readFileSync(svgPath, 'utf-8');

            doc.fontSize(18).fillColor('#dc3545').font('Helvetica-Bold');
            doc.text(`Motor Diagram: ${svgName}`, margin, 120, { width: pageWidth - 2 * margin, align: 'center' });

            const diagramY = 150;
            const diagramWidth = pageWidth - 2 * margin;
            const diagramHeight = 450;
            doc.rect(margin, diagramY, diagramWidth, diagramHeight).stroke('#dee2e6');

            try {
              SVGtoPDF(doc, svgContent, margin + 10, diagramY + 10, {
                width: diagramWidth - 20,
                height: diagramHeight - 20,
                preserveAspectRatio: 'xMidYMid meet',
                assumePt: true
              });
            } catch (svgErr) {
              console.warn(`⚠️ Could not render SVG ${svgName}:`, svgErr.message);
            }
          });

          // Specifications Table Page
          doc.addPage();
          addHeader();
          doc.fontSize(18).fillColor('#dc3545').font('Helvetica-Bold');
          doc.text('Complete Motor Specifications', margin, 120);

          let tableY = 150;
          const tableWidth = pageWidth - 2 * margin;
          const col1Width = tableWidth * 0.45;
          const col2Width = tableWidth * 0.55;
          const rowHeight = 25;

          doc.rect(margin, tableY, tableWidth, rowHeight).fillAndStroke('#dc3545', '#dc3545');
          doc.fontSize(12).fillColor('#ffffff').font('Helvetica-Bold');
          doc.text('Specification', margin + 10, tableY + 8);
          doc.text('Value', margin + col1Width + 10, tableY + 8);

          tableY += rowHeight;

          motorSpecs.forEach((spec, index) => {
            if (tableY + rowHeight > pageHeight - 50) {
              doc.addPage();
              addHeader();
              tableY = 150;
              doc.rect(margin, tableY, tableWidth, rowHeight).fillAndStroke('#dc3545', '#dc3545');
              doc.fontSize(12).fillColor('#ffffff').font('Helvetica-Bold');
              doc.text('Specification', margin + 10, tableY + 8);
              doc.text('Value', margin + col1Width + 10, tableY + 8);
              tableY += rowHeight;
            }
            const fillColor = index % 2 === 0 ? '#f8f9fa' : '#ffffff';
            doc.rect(margin, tableY, tableWidth, rowHeight).fillAndStroke(fillColor, '#dee2e6');
            doc.fontSize(10).fillColor('#000000').font('Helvetica');
            doc.text(spec.spec, margin + 10, tableY + 8, { width: col1Width - 20, ellipsis: true });
            doc.text(spec.value, margin + col1Width + 10, tableY + 8, { width: col2Width - 20, ellipsis: true });
            tableY += rowHeight;
          });

          doc.end();

          pdfBuffer = await new Promise((resolve) => {
            doc.on('end', () => resolve(Buffer.concat(buffers)));
          });

          pdfFilename = `Motor_Diagrams_${customerName.replace(/\s+/g, '_')}_${timestamp}.pdf`;
        }
      } catch (pdfErr) {
        console.error('❌ PDF generation failed:', pdfErr);
      }
    }

    const attachments = [{
      filename: excelFilename,
      content: excelBuffer,
      contentType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    }];

    if (pdfBuffer) {
      attachments.push({
        filename: pdfFilename,
        content: pdfBuffer,
        contentType: 'application/pdf'
      });
    }

    const adminEmailHtml = `
      <div style="font-family: 'Segoe UI', Arial; max-width: 800px; margin: 0 auto; background: white; padding: 30px; border-radius: 8px;">
        <h2 style="color: #dc3545;">⚡ New Motor Inquiry Received</h2>
        <p><strong>Customer:</strong> ${customerName} (${company})</p>
        <p><strong>Email:</strong> ${email} | <strong>Phone:</strong> ${phone}</p>
        <p><strong>Motor:</strong> ${motorDetails.manufacturer || 'N/A'} - ${motorDetails.power || 'N/A'}</p>
        <div style="background: #fff3cd; padding: 12px; border-radius: 6px; margin: 15px 0;">
          <p style="margin: 0;"><strong>📎 Attachments:</strong> Excel spreadsheet ${pdfBuffer ? '+ PDF technical diagrams' : ''}</p>
        </div>
      </div>
    `;

    const userAckHtml = `
      <div style="font-family: Arial; max-width: 500px; margin: 0 auto; padding: 20px; background: white; border-radius: 8px;">
        <h2 style="color: #dc3545; text-align: center;">MR RAWTHER ELTECH</h2>
        <h3 style="color: #28a745; text-align: center;">✅ Thank You!</h3>
        <p>Your motor inquiry has been received. Our team will contact you within 24 hours.</p>
      </div>
    `;

    await Promise.all([
      transporter.sendMail({
        from: `"Motor Inquiry System" <${process.env.EMAIL_USER}>`,
        to: process.env.ADMIN_EMAIL,
        subject: `⚡ New Motor Inquiry from ${customerName} - ${company}`,
        html: adminEmailHtml,
        attachments
      }),
      transporter.sendMail({
        from: `"MR RAWTHER ELTECH - Motors" <${process.env.EMAIL_USER}>`,
        to: email,
        subject: `Thank you for your motor inquiry - MR RAWTHER ELTECH`,
        html: userAckHtml,
        attachments: [{
          filename: excelFilename,
          content: excelBuffer,
          contentType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
        }]
      })
    ]);

    res.json({
      message: `Motor inquiry sent successfully`,
      excelFilename,
      pdfFilename
    });

  } catch (err) {
    console.error('❌ Motor inquiry processing error:', err);
    res.status(500).json({ error: 'Failed to process motor inquiry' });
  }
};

// 💬 Contact Form Handler
const contactForm = async (req, res) => {
  try {
    const { name, email, subject, message } = req.body;
    if (!name || !email || !subject || !message) {
      return res.status(400).json({ success: false, message: 'All fields are required' });
    }

    const adminMailOptions = {
      from: process.env.EMAIL_USER,
      to: process.env.ADMIN_EMAIL,
      subject: `New Contact Form Submission: ${subject}`,
      html: `
        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; color: #333;">
          <h2 style="background-color: #cc0000; color: white; padding: 15px; text-align: center;">New Contact Form Submission</h2>
          <div style="padding: 20px; background-color: #f9f9f9;">
            <p><strong>Name:</strong> ${name}</p>
            <p><strong>Email:</strong> ${email}</p>
            <p><strong>Subject:</strong> ${subject}</p>
            <p><strong>Message:</strong></p>
            <div style="background: white; border-left: 4px solid #cc0000; padding: 15px;">${message.replace(/\n/g, '<br>')}</div>
          </div>
        </div>
      `
    };

    const userMailOptions = {
      from: process.env.EMAIL_USER,
      to: email,
      subject: `Thank you for contacting us: ${subject}`,
      html: `
        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; color: #333;">
          <h2 style="background-color: #cc0000; color: white; padding: 15px; text-align: center;">Thank You for Reaching Out</h2>
          <div style="padding: 20px; background-color: #f9f9f9;">
            <p>Hello ${name},</p>
            <p>We have received your message regarding "${subject}". Our team will review your inquiry and get back to you shortly.</p>
          </div>
        </div>
      `
    };

    await Promise.all([
      transporter.sendMail(adminMailOptions),
      transporter.sendMail(userMailOptions)
    ]);

    res.status(200).json({
      success: true,
      message: 'Your message has been received. We have sent you a confirmation email.'
    });
  } catch (error) {
    console.error('Error processing contact form:', error);
    res.status(500).json({
      success: false,
      message: 'An error occurred while processing your request.'
    });
  }
};

module.exports = {
  sendCode,
  verifyCode,
  productInfo,
  motorInquiry,
  contactForm
};
