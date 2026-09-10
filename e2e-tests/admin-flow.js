const puppeteer = require('puppeteer');
const path = require('path');

const ARTIFACT_DIR = 'C:\\Users\\LEGION 5 PRO\\.gemini\\antigravity-ide\\brain\\a79711c3-e256-41d6-a0cd-dab5787cdddf';

async function delay(time) {
  return new Promise(function(resolve) { 
      setTimeout(resolve, time)
  });
}

async function takeScreenshot(page, name) {
    const filePath = path.join(ARTIFACT_DIR, name);
    await page.screenshot({ path: filePath, fullPage: true });
    console.log(`Saved screenshot: ${filePath}`);
}

(async () => {
  console.log('Launching browser for Admin Flow...');
  const browser = await puppeteer.launch({ headless: 'new', args: ['--window-size=1707,932'] });
  const page = await browser.newPage();
  await page.setViewport({ width: 1707, height: 932 });

  try {
    console.log('1. Navigating to login...');
    await page.goto('http://localhost:4200/login', { waitUntil: 'networkidle2' });
    
    console.log('2. Entering credentials for admin...');
    await page.type('input[formControlName="username"]', 'tiki-admin');
    await page.type('input[formControlName="password"]', '123456');
    await page.click('button[type="submit"]');
    
    console.log('3. Waiting for login and redirect to admin dashboard...');
    await delay(3000);
    await takeScreenshot(page, 'admin_dashboard.png');

    console.log('4. Navigating to admin products...');
    await page.goto('http://localhost:4200/admin/products', { waitUntil: 'networkidle2' });
    await delay(1000);
    await takeScreenshot(page, 'admin_products.png');

    console.log('5. Navigating to admin flash sales...');
    await page.goto('http://localhost:4200/admin/flash-sales', { waitUntil: 'networkidle2' });
    await delay(1000);
    await takeScreenshot(page, 'admin_flash_sales.png');

    console.log('6. Navigating to admin sellers...');
    await page.goto('http://localhost:4200/admin/sellers', { waitUntil: 'networkidle2' });
    await delay(1000);
    await takeScreenshot(page, 'admin_sellers.png');

  } catch (error) {
    console.error('Test failed:', error);
  } finally {
    console.log('Closing browser...');
    await browser.close();
  }
})();
