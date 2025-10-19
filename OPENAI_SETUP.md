# OpenAI ChatGPT API Integration Setup

## 🚀 Overview

The AI Content Generator now supports real ChatGPT API integration! You can generate high-quality articles, video scripts, and photo captions using OpenAI's GPT-4 model.

## 🔧 Setup Instructions

### 1. Get Your OpenAI API Key

1. Visit [OpenAI Platform](https://platform.openai.com/)
2. Sign up or log in to your account
3. Navigate to **API Keys** section
4. Click **"Create new secret key"**
5. Copy your API key (starts with `sk-...`)

### 2. Configure the API Key

You have several options to configure your API key:

#### Option A: Environment Variable (Recommended)
```bash
export OPENAI_API_KEY="your-api-key-here"
```

#### Option B: Application Properties
Add to `src/main/resources/application.properties`:
```properties
openai.api.key=your-api-key-here
```

#### Option C: System Environment
Set as a system environment variable:
- **Windows**: `set OPENAI_API_KEY=your-api-key-here`
- **macOS/Linux**: `export OPENAI_API_KEY=your-api-key-here`

### 3. Restart the Application

After setting the API key, restart your Spring Boot application to load the new configuration.

## 🎯 Features

### Content Types Supported

1. **📝 Articles**
   - Comprehensive articles (800-1200 words)
   - Clear structure with headings
   - Practical tips and advice
   - Warm, supportive tone for parents

2. **🎥 Video Scripts**
   - 60-90 second video scripts
   - Engaging hooks and dialogue
   - Visual cues and actions
   - Strong call-to-actions

3. **📸 Photo Captions**
   - Social media captions (under 200 characters)
   - Relevant hashtags (5-8 hashtags)
   - Engaging opening lines
   - Call-to-actions

### AI Tools Available

- **🤖 ChatGPT**: Real OpenAI GPT-4 integration
- **🧠 Claude**: Mock data (Anthropic's AI assistant)
- **🍌 Nano Banana**: Mock data (Lightweight AI)
- **🎬 Sora**: Mock data (Video generation AI)

## 🔍 How to Use

1. **Navigate** to Admin Panel → AI Content Generator
2. **Check Status**: Look for the API status indicator at the top
   - ✅ **Ready**: API key is configured and working
   - ⚠️ **Not Configured**: API key is missing
3. **Select Content Type**: Choose Article, Video, or Photo
4. **Enter Topic**: Describe what you want to generate content about
5. **Choose Community**: Select target community
6. **Select AI Tool**: Choose ChatGPT for real API integration
7. **Generate**: Click "Generate Content" to create with real ChatGPT

## 📊 API Status Indicator

The system will show you the current API status:

- **✅ Ready**: OpenAI API is configured and ready to use
- **⚠️ Not Configured**: OpenAI API key is not configured

## 🔒 Security Notes

- **Never commit API keys** to version control
- **Use environment variables** for production deployments
- **Keep your API key secure** and don't share it
- **Monitor your usage** on the OpenAI platform to avoid unexpected charges

## 💡 Tips for Better Results

### For Articles:
- Be specific about your topic (e.g., "bedtime routines for toddlers" vs "parenting")
- Mention your target audience (e.g., "first-time parents", "working mothers")
- Include any specific requirements or focus areas

### For Video Scripts:
- Specify the video length (e.g., "60-second educational video")
- Mention the platform (e.g., "Instagram Reels", "YouTube Shorts")
- Include any visual elements you want to highlight

### For Photo Captions:
- Be clear about the photo content
- Mention the social media platform
- Specify the tone (e.g., "encouraging", "educational", "fun")

## 🛠️ Troubleshooting

### API Key Not Working
1. Verify the API key is correct and starts with `sk-`
2. Check that the environment variable is set correctly
3. Restart the application after setting the API key
4. Check the OpenAI platform for any usage limits or billing issues

### Content Generation Fails
1. Check the API status indicator
2. Verify your OpenAI account has sufficient credits
3. Try a simpler topic description
4. Check the application logs for error details

### Fallback Behavior
- If ChatGPT API is not configured, the system will automatically use mock data generation
- Other AI tools (Claude, Nano Banana, Sora) will always use mock data
- The system gracefully handles API failures and shows appropriate error messages

## 📈 Cost Considerations

- **GPT-4 pricing**: Approximately $0.03 per 1K input tokens, $0.06 per 1K output tokens
- **Typical costs**: 
  - Articles: ~$0.10-0.20 per article
  - Video scripts: ~$0.05-0.10 per script
  - Photo captions: ~$0.01-0.02 per caption
- **Monitor usage** on the OpenAI platform dashboard

## 🔄 Future Enhancements

- Support for additional OpenAI models (GPT-3.5-turbo, etc.)
- Custom prompt templates for different content types
- Batch content generation
- Content quality scoring and optimization
- Integration with other AI providers (Anthropic Claude, Google Bard, etc.)

---

**Happy content generating! 🎉**

For support or questions, check the application logs or contact the development team.
