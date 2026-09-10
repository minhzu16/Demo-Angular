const puppeteer = require('puppeteer');
const path = require('path');
const fs = require('fs');

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
  console.log('Launching browser...');
  const browser = await puppeteer.launch({ headless: 'new', args: ['--window-size=1707,932'] });
  const page = await browser.newPage();
  await page.setViewport({ width: 1707, height: 932 });

  try {
    console.log('1. Navigating to login...');
    await page.goto('http://localhost:4200/login', { waitUntil: 'networkidle2' });
    
    console.log('2. Entering credentials...');
    await page.type('input[formControlName="username"]', 'buyer1');
    await page.type('input[formControlName="password"]', '123456');
    await page.click('button[type="submit"]');
    
    console.log('3. Waiting for login to complete...');
    await delay(3000);
    await takeScreenshot(page, 'buyer_home.png');

    console.log('4. Navigating to product details...');
    await page.goto('http://localhost:4200/products/1', { waitUntil: 'networkidle2' });
    await delay(1000);
    await takeScreenshot(page, 'buyer_product_detail.png');

    console.log('5. Adding to cart...');
    const buttons = await page.$$('button');
    for (const btn of buttons) {
        const text = await page.evaluate(el => el.textContent, btn);
        if (text && text.toLowerCase().includes('giỏ hàng')) {
            await btn.click();
            break;
        }
    }
    await delay(1500);

    console.log('6. Navigating to cart...');
    await page.goto('http://localhost:4200/cart', { waitUntil: 'networkidle2' });
    await delay(1000);
    await takeScreenshot(page, 'buyer_cart.png');

    console.log('8. Navigating to checkout...');
    await page.goto('http://localhost:4200/checkout', { waitUntil: 'networkidle2' });
    await delay(2000);
    await takeScreenshot(page, 'buyer_checkout.png');

    console.log('9. Filling shipping info and placing order...');
    // We try to find input fields and fill them if they exist
    try {
      await page.type('input[formControlName="address"]', '123 Test Street');
    } catch(e) {}
    try {
      await page.type('input[formControlName="phone"]', '0123456789');
    } catch(e) {}
    
    const checkoutBtns = await page.$$('button');
    for (const btn of checkoutBtns) {
        const text = await page.evaluate(el => el.textContent, btn);
        if (text && (text.toLowerCase().includes('đặt hàng') || text.toLowerCase().includes('thanh toán'))) {
            await btn.click();
            break;
        }
    }
    await delay(3000); // Wait for order placement
    await takeScreenshot(page, 'buyer_order_confirmed.png');

    console.log('11. Navigating to orders...');
    await page.goto('http://localhost:4200/orders', { waitUntil: 'networkidle2' });
    await delay(2000);
    await takeScreenshot(page, 'buyer_orders_list.png');

    console.log('12. Navigating to wishlist...');
    await page.goto('http://localhost:4200/wishlist', { waitUntil: 'networkidle2' });
    await delay(1000);
    await takeScreenshot(page, 'buyer_wishlist.png');

    console.log('13. Navigating to profile...');
    await page.goto('http://localhost:4200/profile', { waitUntil: 'networkidle2' });
    await delay(1000);
    await takeScreenshot(page, 'buyer_profile.png');

  } catch (error) {
    console.error('Test failed:', error);
  } finally {
    console.log('Closing browser...');
    await browser.close();
  }
})();
