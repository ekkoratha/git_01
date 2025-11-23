document.addEventListener('DOMContentLoaded', () => {
    const canvas = document.getElementById('matrix');
    const ctx = canvas.getContext('2d');
    const banner = document.querySelector('.banner');

    // Set canvas to full window size
    function resize() {
        canvas.width = window.innerWidth;
        canvas.height = window.innerHeight;
        
        // Adjust banner size based on viewport
        const fontSize = Math.min(window.innerWidth, window.innerHeight) * 0.08;
        banner.style.fontSize = `${fontSize}px`;
    }
    
    // Initial resize
    resize();
    
    // Handle window resize
    window.addEventListener('resize', resize);

    // Matrix characters - Katakana and Latin characters
    const matrix = "アァカサタナハマヤャラワガザダバパイィキシチニヒミリヰギジヂビピウゥクスツヌフムユュルグズブヅプエェケセテネヘメレヱゲゼデベペオォコソトノホモヨョロヲゴゾドボポヴッン";
    const fontSize = 16;
    const columns = Math.floor(canvas.width / fontSize);
    
    // Set up the drops array
    const drops = [];
    for (let i = 0; i < columns; i++) {
        drops[i] = Math.random() * -100;
    }

    // Set the font style
    ctx.font = `${fontSize}px monospace`;
    
    // Set the text colors (green shades)
    const colors = ['#0F0', '#0A0', '#0D0', '#0C0'];
    
    // Draw the characters
    function draw() {
        // Semi-transparent black overlay for trail effect
        // More transparent to make the banner more visible
        ctx.fillStyle = 'rgba(0, 0, 0, 0.03)';
        ctx.fillRect(0, 0, canvas.width, canvas.height);
        
        // Loop over drops
        for (let i = 0; i < drops.length; i++) {
            const x = i * fontSize;
            const y = drops[i] * fontSize;
            
            // Random character to print
            const text = matrix[Math.floor(Math.random() * matrix.length)];
            
            // Random color from our palette
            const color = colors[Math.floor(Math.random() * colors.length)];
            ctx.fillStyle = color;
            
            // Draw the character
            ctx.fillText(text, x, y);
            
            // Reset drop to top when it reaches bottom or randomly
            if (y > canvas.height && Math.random() > 0.975) {
                drops[i] = 0;
            }
            
            // Move the drop down
            drops[i]++;
        }
    }

    // Handle window resize
    window.addEventListener('resize', () => {
        canvas.width = window.innerWidth;
        canvas.height = window.innerHeight;
        // Recalculate columns
        columns = Math.floor(canvas.width / fontSize);
        // Reset drops array
        drops.length = 0;
        for (let i = 0; i < columns; i++) {
            drops[i] = Math.floor(Math.random() * -100);
        }
    });

    // Animation loop
    setInterval(draw, 33); // ~30 FPS
});
